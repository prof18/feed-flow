"""Unit tests for the desktop part of the E2E release runner. Run with:

python3 -m unittest discover -s .ai/skills/run-e2e-release-tests/scripts -p 'test_*.py'
"""

from __future__ import annotations

import fcntl
import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import desktop_e2e  # noqa: E402


def load_runner():
    spec = importlib.util.spec_from_file_location("run_e2e_release_tests", SCRIPTS_DIR / "run_e2e_release_tests.py")
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules["run_e2e_release_tests"] = module
    spec.loader.exec_module(module)
    return module


runner = load_runner()

SMOKE_XML = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.prof18.feedflow.desktop.e2e.smoke.SmokeFlowsTest" tests="2" failures="1">
  <testcase name="SM-001 first launch empty" classname="com.prof18.feedflow.desktop.e2e.smoke.SmokeFlowsTest" time="2.5"/>
  <testcase name="SM-002 seeded timeline loads" classname="com.prof18.feedflow.desktop.e2e.smoke.SmokeFlowsTest" time="3.0">
    <failure message="Timed out waiting for text">java.lang.AssertionError: Timed out waiting for text</failure>
  </testcase>
  <system-out>app output</system-out>
  <system-err>app errors</system-err>
</testsuite>
"""

REGRESSION_XML = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.prof18.feedflow.desktop.e2e.regression.LibraryFlowsTest" tests="1" skipped="1">
  <testcase name="REG-101 add feed form validation" classname="com.prof18.feedflow.desktop.e2e.regression.LibraryFlowsTest" time="0.0">
    <skipped message="The desktop E2E suite runs on macOS only"/>
  </testcase>
</testsuite>
"""

HARNESS_XML = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.prof18.feedflow.desktop.e2e.harness.FixtureServerTest" tests="1">
  <testcase name="serves fixture feeds" classname="com.prof18.feedflow.desktop.e2e.harness.FixtureServerTest" time="0.2"/>
</testsuite>
"""


def write_results(folder: Path) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    (folder / "TEST-com.prof18.feedflow.desktop.e2e.smoke.SmokeFlowsTest.xml").write_text(SMOKE_XML)
    (folder / "TEST-com.prof18.feedflow.desktop.e2e.regression.LibraryFlowsTest.xml").write_text(REGRESSION_XML)
    (folder / "TEST-com.prof18.feedflow.desktop.e2e.harness.FixtureServerTest.xml").write_text(HARNESS_XML)


class ParseJunitDirTest(unittest.TestCase):
    def test_parses_flow_cases_and_ignores_the_harness(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            write_results(folder)

            cases = desktop_e2e.parse_junit_dir(folder)

        self.assertEqual(3, len(cases))
        by_name = {case.name: case for case in cases}
        passing = by_name["SM-001 first launch empty"]
        self.assertEqual("smoke", passing.suite)
        self.assertEqual("SmokeFlowsTest", passing.class_name)
        self.assertEqual("SM-001", passing.flow_id)
        self.assertTrue(passing.passed)
        self.assertFalse(passing.skipped)
        self.assertAlmostEqual(2.5, passing.seconds)

        failing = by_name["SM-002 seeded timeline loads"]
        self.assertFalse(failing.passed)
        self.assertIn("Timed out waiting for text", failing.details)
        self.assertIn("app errors", failing.details)

        skipped = by_name["REG-101 add feed form validation"]
        self.assertEqual("regression", skipped.suite)
        self.assertTrue(skipped.skipped)

    def test_missing_folder_gives_no_cases(self) -> None:
        self.assertEqual([], desktop_e2e.parse_junit_dir(Path("/nonexistent/desktop-e2e-results")))


class RunnerDesktopTest(unittest.TestCase):
    def test_all_platforms_and_suites_include_desktop(self) -> None:
        self.assertEqual(("android", "ios", "desktop"), runner.selected_platforms("all"))
        self.assertIn("desktoponly", runner.selected_suites("all"))

    def test_desktop_flow_results(self) -> None:
        passed = desktop_e2e.DesktopCase("smoke", "SmokeFlowsTest", "SM-001 first launch empty", "SM-001", 2.0, True, False, "")
        skipped = desktop_e2e.DesktopCase("smoke", "SmokeFlowsTest", "SM-002 skipped", "SM-002", 0.0, False, True, "")
        failed = desktop_e2e.DesktopCase("smoke", "SmokeFlowsTest", "SM-003 broken", "SM-003", 1.0, False, False, "boom")
        with tempfile.TemporaryDirectory() as tmp:
            results = runner.desktop_flow_results([passed, skipped, failed], Path(tmp))

            self.assertEqual(["desktop"] * 3, [result.platform for result in results])
            self.assertEqual(Path("desktop/smoke/SmokeFlowsTest/SM-001 first launch empty"), results[0].flow_path)
            self.assertEqual([0, 0, 1], [result.returncode for result in results])
            self.assertEqual([False, True, False], [result.skipped for result in results])
            self.assertIn("boom", results[2].log_path.read_text())
            self.assertEqual("SKIP", runner.result_label(results[1]))
            self.assertEqual("skipped", runner.result_class(results[1]))
            self.assertEqual("PASS", runner.result_label(results[0]))
            self.assertEqual("FAIL", runner.result_label(results[2]))
            self.assertEqual("NOT RUN", runner.result_label(None))

    def test_methods_sharing_an_id_keep_separate_logs(self) -> None:
        card = desktop_e2e.DesktopCase("regression", "ArticleReaderFlowsTest", "REG-108 card layout", "REG-108", 1.0, False, False, "card")
        grid = desktop_e2e.DesktopCase("regression", "ArticleReaderFlowsTest", "REG-108 grid layout", "REG-108", 1.0, False, False, "grid")
        with tempfile.TemporaryDirectory() as tmp:
            results = runner.desktop_flow_results([card, grid], Path(tmp))
            self.assertNotEqual(results[0].log_path, results[1].log_path)
            self.assertIn("card", results[0].log_path.read_text())

    def test_summary_keeps_planned_mobile_suites_without_results(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "e2e/maestro/android/smoke").mkdir(parents=True)
            (root / "e2e/maestro/android/smoke/001-first.yaml").write_text("appId: x")
            rows = runner.suite_summary_rows([], ("android", "desktop"), ("smoke", "desktoponly"), root)
        self.assertEqual([("android", "smoke", 0, 0, 0, 0)], rows)

    def test_setup_failed_detection(self) -> None:
        crash = "Process 'Gradle Test Executor 3' finished with non-zero exit value 137"
        self.assertTrue(runner.desktop_setup_failed(returncode=1, log_text=crash, failed_cases=0))
        self.assertFalse(runner.desktop_setup_failed(1, "There were failing tests", 2))
        self.assertTrue(runner.desktop_setup_failed(1, "Compilation failed", 0))
        self.assertFalse(runner.desktop_setup_failed(0, "", 0))

    @unittest.skipUnless(sys.platform == "darwin", "the desktop runner runs on macOS only")
    def test_second_runner_is_refused_while_the_lock_is_held(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            evidence = root / "evidence"
            evidence.mkdir()
            marker = evidence / "keep.txt"
            marker.write_text("previous run")
            lock = root / "runner.lock"
            with lock.open("w") as held:
                fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
                result = runner.run_desktop_tests(
                    root,
                    root / "out",
                    ("smoke",),
                    os.environ.copy(),
                    lock_path=lock,
                    run_lock_path=root / "run.lock",
                    junit_dir=root / "junit",
                    evidence_dir=evidence,
                    gradle_command=["false"],
                )
            self.assertEqual(1, len(result.setup_results))
            self.assertNotEqual(0, result.setup_results[0].returncode)
            self.assertIn("another desktop e2e runner", result.setup_results[0].log_path.read_text().lower())
            self.assertTrue(marker.exists())
            self.assertEqual([], result.flow_results)

    @unittest.skipUnless(sys.platform == "darwin", "the desktop runner runs on macOS only")
    def test_active_direct_run_keeps_its_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            evidence = root / "evidence"
            evidence.mkdir()
            marker = evidence / "keep.txt"
            marker.write_text("active run")
            run_lock = root / "run.lock"
            # The test JVM holds run.lock with a POSIX lock (FileChannel.tryLock), owned by another process.
            holder = subprocess.Popen(
                [
                    sys.executable,
                    "-c",
                    "import fcntl, sys, time\n"
                    "f = open(sys.argv[1], 'w')\n"
                    "fcntl.lockf(f, fcntl.LOCK_EX | fcntl.LOCK_NB)\n"
                    "print('locked', flush=True)\n"
                    "time.sleep(30)\n",
                    str(run_lock),
                ],
                stdout=subprocess.PIPE,
                text=True,
            )
            try:
                self.assertEqual("locked", holder.stdout.readline().strip())
                result = runner.run_desktop_tests(
                    root,
                    root / "out",
                    ("smoke",),
                    os.environ.copy(),
                    lock_path=root / "runner.lock",
                    run_lock_path=run_lock,
                    junit_dir=root / "junit",
                    evidence_dir=evidence,
                    gradle_command=["false"],
                )
            finally:
                holder.kill()
                holder.wait()
            self.assertEqual(1, len(result.setup_results))
            self.assertNotEqual(0, result.setup_results[0].returncode)
            self.assertIn("desktop e2e run is active", result.setup_results[0].log_path.read_text().lower())
            self.assertTrue(marker.exists())

    @unittest.skipUnless(sys.platform == "darwin", "the desktop runner runs on macOS only")
    def test_gradle_failure_without_results_is_one_setup_failure(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            result = runner.run_desktop_tests(
                root,
                root / "out",
                ("smoke",),
                os.environ.copy(),
                lock_path=root / "runner.lock",
                run_lock_path=root / "run.lock",
                junit_dir=root / "junit",
                evidence_dir=root / "evidence",
                gradle_command=["sh", "-c", "echo Compilation failed; exit 1"],
            )
        self.assertEqual(1, len(result.setup_results))
        self.assertEqual(1, result.setup_results[0].returncode)
        self.assertEqual([], result.flow_results)

    @unittest.skipUnless(sys.platform == "darwin", "the desktop runner runs on macOS only")
    def test_main_with_desktop_platform_writes_reports(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            junit = root / "junit"

            def fake_gradle(*args, **kwargs):
                write_results(junit)
                return ["sh", "-c", "echo There were failing tests; exit 1"]

            argv = ["run_e2e_release_tests.py", "--platform", "desktop", "--suite", "smoke", "--output-dir", str(root / "out")]
            with mock.patch.object(sys, "argv", argv), mock.patch.object(
                runner, "DESKTOP_JUNIT_RESULTS_DIR", junit
            ), mock.patch.object(runner, "DESKTOP_EVIDENCE_DIR", root / "evidence"), mock.patch.object(
                runner, "DESKTOP_RUNNER_LOCK", root / "runner.lock"
            ), mock.patch.object(runner, "DESKTOP_RUN_LOCK", root / "run.lock"), mock.patch.object(
                runner, "desktop_gradle_command", side_effect=fake_gradle
            ), mock.patch.object(runner, "find_repo_root", return_value=root):
                exit_code = runner.main()

            report = (root / "out" / "report.md").read_text()
            self.assertEqual(1, exit_code)
            self.assertIn("desktop", report)
            self.assertIn("SM-001 first launch empty", report)
            self.assertTrue((root / "out" / "report.html").exists())


if __name__ == "__main__":
    unittest.main()
