import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "e2e/scripts/android_reader_tooltip.py"
FLOW = ROOT / "e2e/maestro/android/smoke/007-reader-mode-core.yaml"
SPEC = importlib.util.spec_from_file_location("android_reader_tooltip", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class SplitFlowTest(unittest.TestCase):
    def test_preserves_original_setup_assertions_presence_and_complete_tail(self):
        source = FLOW.read_text()
        header, setup, presence, tail = MODULE.split_flow(source)

        self.assertEqual(header, "appId: com.prof18.feedflow.debug\n---\n")
        self.assertIn("- launchApp", setup)
        self.assertIn("feedflow://e2e/reset-and-seed?profile=reader-mode", setup)
        self.assertIn("- assertVisible:\n    id: reader_browser_button", setup)
        self.assertNotIn("- swipe:", setup)
        self.assertIn("- assertVisible:\n    id: reader_toolbar_tooltip", presence)
        self.assertTrue(tail.startswith("- extendedWaitUntil:\n    notVisible:"))
        self.assertIn("- tapOn:\n    id: reader_next_button", tail)
        self.assertIn("- tapOn:\n    id: reader_bookmark_button", tail)
        self.assertIn('text: "Text Settings"', tail)
        self.assertIn('- assertVisible: "Line Height"', tail)

    def test_rejects_missing_or_nonstationary_gesture_and_bad_dismissal(self):
        source = FLOW.read_text()
        bad_sources = (
            source.replace("duration: 800", "duration: 700"),
            source.replace("end: 21%, 89%", "end: 22%, 89%"),
            source.replace("- swipe:\n", "- tapOn:\n", 1),
            source.replace("notVisible:\n      id: reader_toolbar_tooltip", "notVisible:\n      id: another_tooltip"),
        )
        for malformed in bad_sources:
            with self.subTest(source=malformed[-180:]):
                with self.assertRaises(ValueError):
                    MODULE.split_flow(malformed)

    def test_rejects_duplicate_tooltip_presence(self):
        source = FLOW.read_text()
        assertion = '- assertVisible:\n    id: reader_toolbar_tooltip\n'
        with self.assertRaises(ValueError):
            MODULE.split_flow(source.replace(assertion, assertion + assertion))

    def test_rejects_lost_seed_assertion_or_reader_action_tail(self):
        source = FLOW.read_text()
        missing_seed = source.replace('- assertVisible: "E2E seed complete"\n', "")
        missing_tail = source.split("- tapOn:\n    id: reader_bookmark_button", 1)[0]
        for malformed in (missing_seed, missing_tail):
            with self.subTest(source=malformed[-180:]):
                with self.assertRaises(ValueError):
                    MODULE.split_flow(malformed)


def layout(*, enabled=True, long_clickable=True, center=(226, 2222), bounds="[163,2159][289,2285]"):
    interactions = ["CLICKABLE"]
    if long_clickable:
        interactions.append("LONG_CLICKABLE")
    return {
        "windows": [{
            "content": [{
                "resource-id": "reader_browser_button",
                "bounds": bounds,
                "center": list(center),
                "interactions": interactions,
                "state": {"enabled": enabled},
            }],
        }],
    }


class ButtonCenterTest(unittest.TestCase):
    def test_uses_observed_enabled_long_clickable_center(self):
        self.assertEqual(MODULE.button_center(layout()), (226, 2222))

    def test_accepts_android_cli_string_center_when_enabled_is_corroborated(self):
        node = layout()
        target = node["windows"][0]["content"][0]
        target["center"] = "[226,2222]"
        target.pop("state")
        target["enabled"] = True
        self.assertEqual(MODULE.button_center(node), (226, 2222))

    def test_rejects_absent_ambiguous_disabled_or_non_long_clickable_target(self):
        cases = (
            {"windows": [{"content": []}]},
            {"windows": [{"content": [layout()["windows"][0]["content"][0]] * 2}]},
            layout(enabled=False),
            layout(long_clickable=False),
            layout(enabled=True) | {
                "windows": [{"content": [{
                    **layout()["windows"][0]["content"][0],
                    "state": {"enabled": True, "flags": ["DISABLED"]},
                }]}],
            },
        )
        for value in cases:
            with self.subTest(layout=value):
                with self.assertRaises(ValueError):
                    MODULE.button_center(value)


class FakeRunner:
    def __init__(self, fail_presence=False, *, cli_layout=None, native_xml=None):
        self.fail_presence = fail_presence
        self.cli_layout = cli_layout
        self.native_xml = native_xml
        self.commands = []

    def run(self, command, log, timeout):
        self.commands.append(command)
        log.write_text("fake command output\n")
        if command[0] == "android":
            destination = next(arg.split("=", 1)[1] for arg in command if arg.startswith("--output="))
            value = self.cli_layout if self.cli_layout is not None else layout()
            if self.cli_layout is None:
                node = value["windows"][0]["content"][0]
                node.pop("state")
                node["center"] = "[226,2222]"
            Path(destination).write_text(json.dumps(value))
        if "exec-out" in command:
            native_xml = self.native_xml or (
                '<hierarchy><node package="com.prof18.feedflow.debug" resource-id="reader_article_e2e_article_reader_success">'
                '<node package="com.prof18.feedflow.debug" resource-id="reader_browser_button" '
                'bounds="[163,2159][289,2285]" enabled="true" clickable="true" long-clickable="true" />'
                '</node></hierarchy>'
            )
            return subprocess.CompletedProcess(command, 0, native_xml, "")
        if self.fail_presence and command[-1].endswith("presence.yaml"):
            return subprocess.CompletedProcess(command, 1, "", "assertion failed")
        return subprocess.CompletedProcess(command, 0, "", "")


class GestureCleanupTest(unittest.TestCase):
    def test_native_layout_selects_unique_button_when_cli_only_has_system_ui(self):
        runner = FakeRunner(cli_layout=[{
            "package": "com.android.systemui",
            "content": [{"resource-id": "status_bar", "bounds": "[0,0][1080,80]"}],
        }])
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"
            result = MODULE.run_journey("test-device", FLOW, output, runner=runner, sleep=lambda _: None)
            self.assertTrue(result["passed"], result.get("error"))
            self.assertEqual((226, 2222), result["point"])
            self.assertEqual("native-uiautomator", result["coordinateSource"])
            self.assertTrue(any("motionevent" in command and command[-3] == "DOWN" for command in runner.commands))
            self.assertTrue(any("motionevent" in command and command[-3] == "UP" for command in runner.commands))
            header, _, presence, tail = MODULE.split_flow(FLOW.read_text())
            self.assertEqual(header + presence, (output / "presence.yaml").read_text())
            self.assertEqual(header + tail, (output / "tail.yaml").read_text())

    def test_native_selection_rejects_untrusted_or_ambiguous_hierarchy(self):
        valid_target = (
            '<node package="com.prof18.feedflow.debug" resource-id="reader_browser_button" '
            'bounds="[163,2159][289,2285]" enabled="true" clickable="true" long-clickable="true" />'
        )
        reader = '<node package="com.prof18.feedflow.debug" resource-id="reader_article_e2e_article_reader_success">{}</node>'
        cases = {
            "wrong app": f'<hierarchy>{reader.format(valid_target.replace("package=\"com.prof18.feedflow.debug\"", "package=\"other.app\""))}</hierarchy>',
            "missing reader": f'<hierarchy>{valid_target}</hierarchy>',
            "not long-clickable": f'<hierarchy>{reader.format(valid_target.replace("long-clickable=\"true\"", "long-clickable=\"false\""))}</hierarchy>',
            "disabled": f'<hierarchy>{reader.format(valid_target.replace("enabled=\"true\"", "enabled=\"false\""))}</hierarchy>',
            "malformed bounds": f'<hierarchy>{reader.format(valid_target.replace("[163,2159][289,2285]", "bad"))}</hierarchy>',
            "duplicate native targets": f'<hierarchy>{reader.format(valid_target + valid_target)}</hierarchy>',
        }
        for label, native_xml in cases.items():
            with self.subTest(label=label), tempfile.TemporaryDirectory() as directory:
                runner = FakeRunner(cli_layout=[], native_xml=native_xml)
                result = MODULE.run_journey("test-device", FLOW, Path(directory) / "result", runner=runner,
                                            sleep=lambda _: None)
                self.assertFalse(result["passed"])
                self.assertFalse(any("motionevent" in command for command in runner.commands))

    def test_duplicate_cli_targets_reject_even_with_valid_native_target(self):
        node = layout()["windows"][0]["content"][0]
        runner = FakeRunner(cli_layout=[{"content": [node, dict(node)]}])
        with tempfile.TemporaryDirectory() as directory:
            result = MODULE.run_journey("test-device", FLOW, Path(directory) / "result", runner=runner,
                                        sleep=lambda _: None)
        self.assertFalse(result["passed"])
        self.assertFalse(any("motionevent" in command for command in runner.commands))

    def test_failed_presence_releases_touch_without_running_tail(self):
        runner = FakeRunner(fail_presence=True)
        with tempfile.TemporaryDirectory() as directory:
            result = MODULE.run_journey("test-device", FLOW, Path(directory) / "result", runner=runner, sleep=lambda _: None)
            self.assertFalse(result["passed"])
            self.assertFalse(result["pressActive"])
        inputs = [command for command in runner.commands if "motionevent" in command]
        self.assertEqual(["DOWN", "UP"], [command[-3] for command in inputs])
        self.assertFalse(any(command[-1].endswith("tail.yaml") for command in runner.commands))
        self.assertTrue(all(command[2] == "test-device" for command in inputs))

    def test_success_preserves_assertions_and_original_reader_action_tail(self):
        runner = FakeRunner()
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"
            result = MODULE.run_journey("test-device", FLOW, output, runner=runner, sleep=lambda _: None)
            self.assertTrue(result["passed"])
            self.assertFalse(result["pressActive"])
            self.assertEqual("android-cli", result["coordinateSource"])
            header, setup, presence, tail = MODULE.split_flow(FLOW.read_text())
            self.assertEqual(header + presence, (output / "presence.yaml").read_text())
            self.assertEqual(header + tail, (output / "tail.yaml").read_text())

    def test_failed_release_never_reports_pass_or_runs_reader_tail(self):
        class FailedRelease(FakeRunner):
            def run(self, command, log, timeout):
                completed = super().run(command, log, timeout)
                if "motionevent" in command and command[-3] == "UP":
                    completed.returncode = 1
                return completed

        runner = FailedRelease()
        with tempfile.TemporaryDirectory() as directory:
            result = MODULE.run_journey("test-device", FLOW, Path(directory) / "result", runner=runner, sleep=lambda _: None)
            self.assertFalse(result["passed"])
            self.assertTrue(result["pressActive"])
            self.assertIn("cleanupError", result)
        self.assertFalse(any(command[-1].endswith("tail.yaml") for command in runner.commands))

    def test_disagreeing_native_bounds_rejects_input(self):
        class ChangedBounds(FakeRunner):
            def run(self, command, log, timeout):
                completed = super().run(command, log, timeout)
                if "exec-out" in command:
                    completed.stdout = completed.stdout.replace("2159", "2160")
                return completed

        runner = ChangedBounds()
        with tempfile.TemporaryDirectory() as directory:
            result = MODULE.run_journey("test-device", FLOW, Path(directory) / "result", runner=runner, sleep=lambda _: None)
            self.assertFalse(result["passed"])
        self.assertFalse(any("motionevent" in command for command in runner.commands))

    def test_rejects_invalid_bounds_and_centers_outside_bounds(self):
        for value in (layout(bounds="invalid"), layout(center=(227, 2136)), layout(center="not-a-center")):
            with self.subTest(layout=value):
                with self.assertRaises(ValueError):
                    MODULE.button_center(value)


class CommandInputTest(unittest.TestCase):
    def test_child_command_cannot_consume_the_parent_flow_list(self):
        with tempfile.TemporaryDirectory() as directory:
            code = f"""
import importlib.util
from pathlib import Path
import sys
spec = importlib.util.spec_from_file_location('helper', {str(SCRIPT)!r})
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)
helper.CommandRunner().run([sys.executable, '-c', 'import sys; sys.stdin.read()'], Path({str(Path(directory) / 'child.log')!r}), 5)
print(sys.stdin.read(), end='')
"""
            result = subprocess.run([sys.executable, "-c", code], input="next-flow.yaml\n",
                                    capture_output=True, text=True, timeout=10, check=False)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual("next-flow.yaml\n", result.stdout)


if __name__ == "__main__":
    unittest.main()
