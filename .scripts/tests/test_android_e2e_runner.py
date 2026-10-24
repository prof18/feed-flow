import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class AndroidRunnerTest(unittest.TestCase):
    def run_fixture(self, restore_exit=0, explicit_device=None, flavor=None,
                    serial="owned-emulator", fdroid_overrides=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts = root / "e2e/scripts"
            scripts.mkdir(parents=True)
            shutil.copy(ROOT / "e2e/scripts/run-android.sh", scripts)
            fixture = scripts / "push-android-fixtures.sh"
            fixture.write_text("#!/usr/bin/env bash\nadb shell fixture-copy\n")
            fixture.chmod(0o755)
            flows = root / "e2e/maestro/android/smoke"
            flows.mkdir(parents=True)
            (flows / "007-reader-mode-core.yaml").write_text("fixture")
            (flows / "008-other.yaml").write_text("fixture")
            regression = root / "e2e/maestro/android/regression"
            regression.mkdir()
            (regression / "179-share-existing-feed.yaml").write_text("fixture")
            if fdroid_overrides:
                alternate = root / "e2e/maestro/android/fdroid/regression"
                alternate.mkdir(parents=True)
                for name in (
                    "118-cloud-provider-mock-states.yaml",
                    "132-about-support-secondary-options.yaml",
                    "133-home-sync-backup-action.yaml",
                    "147-no-feeds-empty-state-cta.yaml",
                ):
                    (regression / name).write_text(f"canonical:{name}")
                    (alternate / name).write_text(f"fdroid:{name}")
                (regression / "145-network-provider-linked-states.yaml").write_text("fixture")
                (alternate / "999-orphan.yaml").write_text("must not run")
                alternate_smoke = alternate.parent / "smoke"
                alternate_smoke.mkdir()
                (alternate_smoke / "008-other.yaml").write_text("must not run")
            tools = root / "tools"
            tools.mkdir()
            log = root / "commands.log"
            for name in ("adb", "android", "maestro", "python3"):
                tool = tools / name
                tool.write_text(f'#!/usr/bin/env bash\nprintf "%s|%s|%s\\n" "{name}" "$ANDROID_SERIAL" "$*" >> "$COMMAND_LOG"\n')
                tool.chmod(0o755)
            if explicit_device is not None:
                helper = tools / "python3"
                helper.write_text(f'#!/usr/bin/env bash\nmaestro --device={explicit_device} --platform android test presence.yaml\n')
            restore = tools / "feedflow-restore-dev-feeds"
            restore.write_text(f'#!/usr/bin/env bash\nmaestro --platform android test restore.yaml\nexit {restore_exit}\n')
            restore.chmod(0o755)
            gradle = root / "gradlew"
            gradle.write_text('#!/usr/bin/env bash\nprintf "gradle|%s|%s\\n" "$ANDROID_SERIAL" "$*" >> "$COMMAND_LOG"\n')
            gradle.chmod(0o755)
            environment = dict(os.environ, PATH=f"{tools}:/usr/bin:/bin", ANDROID_SERIAL=serial,
                               E2E_ANDROID_SUITES="smoke regression", COMMAND_LOG=str(log))
            if serial is None:
                environment.pop("ANDROID_SERIAL", None)
            environment.pop("READER_IMAGE_POINT", None)
            environment.pop("E2E_ANDROID_FLAVOR", None)
            if flavor is not None:
                environment["E2E_ANDROID_FLAVOR"] = flavor
            log.write_text("")
            result = subprocess.run(["bash", str(scripts / "run-android.sh")], env=environment,
                                    capture_output=True, text=True, timeout=10, check=False)
            return result, log.read_text()

    def test_selected_device_reaches_install_fixture_reader_and_restore(self):
        result, commands = self.run_fixture()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("android|owned-emulator|run --device=owned-emulator", commands)
        self.assertIn("adb|owned-emulator|shell fixture-copy", commands)
        self.assertIn("python3|owned-emulator|", commands)
        self.assertIn("--device owned-emulator --flow", commands)
        self.assertIn("--parent-restores", commands)
        self.assertIn("maestro|owned-emulator|--device=owned-emulator --platform android test restore.yaml", commands)

    def test_default_flavor_keeps_google_play_build_and_install(self):
        result, commands = self.run_fixture()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("gradle|owned-emulator|--quiet --console=plain :androidApp:assembleGooglePlayDebug", commands)
        self.assertIn("android|owned-emulator|run --device=owned-emulator --apks=", commands)
        self.assertIn("/apk/googlePlay/debug/androidApp-googlePlay-debug.apk", commands)

    def test_share_flow_uses_pinned_native_bridge_and_parent_restoration(self):
        result, commands = self.run_fixture()
        self.assertEqual(0, result.returncode, result.stderr)
        share = next(line for line in commands.splitlines() if "android_share_feed.py" in line)
        self.assertIn("python3|owned-emulator|", share)
        self.assertIn("--device owned-emulator --flow", share)
        self.assertIn("179-share-existing-feed.yaml --output", share)
        self.assertIn("/share-feed --parent-restores", share)
        self.assertIn("test restore.yaml", commands)

    def test_fdroid_flavor_selects_fdroid_build_and_install(self):
        result, commands = self.run_fixture(flavor="fdroid")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("gradle|owned-emulator|--quiet --console=plain :androidApp:assembleFdroidDebug", commands)
        self.assertIn("/apk/fdroid/debug/androidApp-fdroid-debug.apk", commands)

    def test_fdroid_selects_overrides_once_in_canonical_order(self):
        result, commands = self.run_fixture(flavor="fdroid", fdroid_overrides=True)
        self.assertEqual(0, result.returncode, result.stderr)
        names = (
            "118-cloud-provider-mock-states.yaml",
            "132-about-support-secondary-options.yaml",
            "133-home-sync-backup-action.yaml",
            "147-no-feeds-empty-state-cta.yaml",
        )
        lines = commands.splitlines()
        for name in names:
            with self.subTest(flow=name):
                selected = [line for line in lines if name in line]
                self.assertEqual(1, len(selected), selected)
                self.assertIn(f"/android/fdroid/regression/{name}", selected[0])
                self.assertIn("--device=owned-emulator", selected[0])
        executed = [Path(argument).name for line in lines for argument in line.split()
                    if "/regression/" in argument and argument.endswith(".yaml")]
        self.assertEqual(sorted([*names, "145-network-provider-linked-states.yaml",
                                 "179-share-existing-feed.yaml"]), executed)
        self.assertIn("/apk/fdroid/debug/androidApp-fdroid-debug.apk", commands)

    def test_fdroid_preserves_unmatched_flows_helpers_and_smoke(self):
        result, commands = self.run_fixture(flavor="fdroid", fdroid_overrides=True)
        self.assertEqual(0, result.returncode, result.stderr)
        for name in ("008-other.yaml", "145-network-provider-linked-states.yaml",
                     "179-share-existing-feed.yaml"):
            selected = [line for line in commands.splitlines() if name in line]
            self.assertEqual(1, len(selected), selected)
            self.assertNotIn("/fdroid/", selected[0])
        self.assertIn("android_share_feed.py", commands)
        self.assertIn("--parent-restores", commands)
        self.assertNotIn("999-orphan.yaml", commands)
        self.assertIn("test restore.yaml", commands)

    def test_google_play_keeps_canonical_flows_despite_fdroid_overrides(self):
        for flavor in (None, "googlePlay"):
            with self.subTest(flavor=flavor):
                result, commands = self.run_fixture(flavor=flavor, fdroid_overrides=True)
                self.assertEqual(0, result.returncode, result.stderr)
                for name in ("118-cloud-provider-mock-states.yaml",
                             "132-about-support-secondary-options.yaml",
                             "133-home-sync-backup-action.yaml",
                             "147-no-feeds-empty-state-cta.yaml"):
                    selected = [line for line in commands.splitlines() if name in line]
                    self.assertEqual(1, len(selected), selected)
                    self.assertIn(f"/android/regression/{name}", selected[0])
                    self.assertNotIn("/android/fdroid/", selected[0])
                self.assertIn("/apk/googlePlay/debug/androidApp-googlePlay-debug.apk", commands)

    def test_unknown_flavor_exits_before_device_build_install_and_restore(self):
        result, commands = self.run_fixture(flavor="other", serial=None)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Unsupported E2E_ANDROID_FLAVOR", result.stderr)
        self.assertEqual("", commands)

    def test_failed_restore_cannot_report_a_successful_gate(self):
        result, _ = self.run_fixture(restore_exit=1)
        self.assertNotEqual(0, result.returncode)

    def test_helper_explicit_device_is_not_duplicated_by_shim(self):
        result, commands = self.run_fixture(explicit_device="owned-emulator")
        self.assertEqual(0, result.returncode, result.stderr)
        presence = next(line for line in commands.splitlines() if "presence.yaml" in line)
        self.assertEqual(1, presence.count("--device="))

    def test_shim_rejects_a_conflicting_helper_device(self):
        result, commands = self.run_fixture(explicit_device="ordinary-device")
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("presence.yaml", commands)

    def test_ordinary_flow_runs_without_extra_maestro_environment(self):
        result, commands = self.run_fixture()
        self.assertEqual(0, result.returncode, result.stderr)
        normal = next(line for line in commands.splitlines() if "008-other.yaml" in line)
        self.assertIn("--platform android test ", normal)
        self.assertNotIn(" -e ", normal)


if __name__ == "__main__":
    unittest.main()
