import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "e2e/scripts/android_share_feed.py"
FLOW = ROOT / "e2e/maestro/android/regression/179-share-existing-feed.yaml"
SPEC = importlib.util.spec_from_file_location("android_share_feed", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class SplitFlowTest(unittest.TestCase):
    def test_splits_at_single_marker_and_preserves_original_assertions(self):
        source = FLOW.read_text()
        setup, result = MODULE.split_flow(source)

        self.assertTrue(setup.startswith("appId: com.prof18.feedflow.debug\n---\n"))
        self.assertIn("feedflow://e2e/reset-and-seed?profile=content-rich", setup)
        self.assertIn("article_row_e2e_article_newest_unread", setup)
        self.assertTrue(result.startswith("appId: com.prof18.feedflow.debug\n---\n"))
        self.assertIn('- assertVisible: "Share"', result)
        self.assertIn('text: "^FeedFlow$"', result)
        self.assertIn("android:id/button_once", result)
        self.assertIn('Android Central RSS Feed is already in your feeds.', result)
        self.assertIn('- assertNotVisible: "Adding feed…"', result)
        self.assertIn("article_row_e2e_article_newest_unread", result)

    def test_rejects_non_debug_header_duplicate_marker_and_missing_seed(self):
        source = FLOW.read_text()
        marker = MODULE._MARKER
        malformed = {
            "non-debug app header": source.replace(
                "appId: com.prof18.feedflow.debug", "appId: com.prof18.feedflow"
            ),
            "duplicate native marker": source.replace(marker, marker + marker),
            "missing local seed": source.replace(
                "feedflow://e2e/reset-and-seed?profile=content-rich",
                "https://example.test/feed",
            ),
        }
        for label, candidate in malformed.items():
            with self.subTest(label=label), self.assertRaises(ValueError):
                MODULE.split_flow(candidate)

    def test_rejects_missing_duplicate_result_assertion(self):
        source = FLOW.read_text()
        assertion = '- assertVisible: "Android Central RSS Feed is already in your feeds."\n'
        with self.assertRaises(ValueError):
            MODULE.split_flow(source.replace(assertion, ""))


class JourneyCommandTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.flow = FLOW

    def run_with(self, fake):
        return MODULE.run_journey(
            "emulator-test-serial",
            self.flow,
            self.root / "receipt",
            run=fake,
        )

    def test_pins_every_device_command_and_uses_bounded_noninteractive_children(self):
        calls = []

        def fake_run(args, **kwargs):
            calls.append((args, kwargs))
            return subprocess.CompletedProcess(args, 0, "fake stdout\n", "")

        result = self.run_with(fake_run)

        self.assertTrue(result["passed"], result.get("error"))
        self.assertEqual({"release-layout-driver", "setup", "send", "result"},
                         set(result["phases"]))
        for args, kwargs in calls:
            with self.subTest(command=args[0]):
                self.assertIs(kwargs["stdin"], subprocess.DEVNULL)
                self.assertTrue(kwargs["capture_output"])
                self.assertTrue(kwargs["text"])
                self.assertFalse(kwargs["check"])
                self.assertGreater(kwargs["timeout"], 0)
                self.assertLessEqual(kwargs["timeout"], 60)
                if args[0] == "adb":
                    self.assertEqual("-s", args[1])
                    self.assertEqual("emulator-test-serial", args[2])
                elif args[0] == "maestro":
                    self.assertIn("--device=emulator-test-serial", args)

        saved = json.loads((self.root / "receipt/result.json").read_text())
        self.assertTrue(saved["passed"])

    def test_failed_share_command_never_reports_pass_or_runs_result_phase(self):
        calls = []

        def fake_run(args, **kwargs):
            calls.append(args)
            if args[0] == "adb" and "android.intent.action.SEND" in args:
                return subprocess.CompletedProcess(args, 1, "share failed", "resolver error")
            return subprocess.CompletedProcess(args, 0, "ok", "")

        result = self.run_with(fake_run)

        self.assertFalse(result["passed"])
        self.assertEqual(1, result["phases"]["send"])
        self.assertNotIn("result", result["phases"])
        self.assertFalse(any(args[0] == "maestro" and any(
            str(arg).endswith("result.yaml") for arg in args
        ) for args in calls))
        self.assertFalse((self.root / "receipt/result.yaml").exists())
        saved = json.loads((self.root / "receipt/result.json").read_text())
        self.assertFalse(saved["passed"])


if __name__ == "__main__":
    unittest.main()
