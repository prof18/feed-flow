import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WATCHDOG = ROOT / ".scripts" / "ci-test-stall-watchdog.sh"
BASH = shutil.which("bash")
PATTERN = "FeedFlowStallWatchdogFixture"


class CiTestStallWatchdogTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="feedflow-stall-test-")
        self.work = Path(self.temp.name)
        self.bin = self.work / "bin"
        self.bin.mkdir()
        self.progress = self.work / "progress.bin"
        self.progress.write_text("start")
        self.dumps = self.work / "dumps"
        self.jcmd_calls = self.work / "jcmd-calls"
        jcmd = self.bin / "jcmd"
        jcmd.write_text(
            f"#!{sys.executable}\n"
            "import os, sys\n"
            f"open({str(self.jcmd_calls)!r}, 'a').write(' '.join(sys.argv[1:]) + '\\n')\n"
            "print('\"main\" fixture thread waiting')\n"
        )
        jcmd.chmod(0o755)
        self.fixture = subprocess.Popen(
            [sys.executable, "-c", "import time; time.sleep(60)", PATTERN],
        )
        self.addCleanup(self._stop, self.fixture)

    def tearDown(self):
        self.temp.cleanup()

    def _stop(self, process):
        if process.poll() is None:
            process.kill()
            process.wait()

    def _run_watchdog(self, seconds, touch_progress=False, until=None):
        """Runs the watchdog for `seconds`, or until `until` appears in its output."""
        env = dict(
            os.environ,
            PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}",
            STALL_WATCH_FILE=str(self.progress),
            STALL_SECONDS="2",
            STALL_POLL_SECONDS="1",
            STALL_DUMP_DIR=str(self.dumps),
            STALL_PROCESS_PATTERN=PATTERN,
        )
        watchdog = subprocess.Popen(
            [BASH, str(WATCHDOG)], env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        )
        lines = []
        reader = threading.Thread(target=lambda: lines.extend(iter(watchdog.stdout.readline, "")), daemon=True)
        reader.start()
        deadline = time.monotonic() + seconds
        counter = 0
        while time.monotonic() < deadline and not (until and until in "".join(lines)):
            if touch_progress:
                counter += 1
                self.progress.write_text(f"progress {counter}")
            time.sleep(0.5)
        watchdog.terminate()
        watchdog.wait(timeout=10)
        reader.join(timeout=10)
        return "".join(lines)

    def test_stalled_tests_print_a_thread_dump_without_stopping_the_jvm(self):
        # Loaded CI runners can take a while to report the stall, so wait for the whole dump.
        output = self._run_watchdog(seconds=60, until="::endgroup::")

        self.assertIn("::warning::Desktop tests made no progress", output)
        self.assertIn(f"Thread dump of test JVM {self.fixture.pid}", output)
        self.assertIn("fixture thread waiting", output)
        self.assertIn(f"{self.fixture.pid} Thread.print", self.jcmd_calls.read_text())
        self.assertTrue(any(self.dumps.iterdir()))
        self.assertIsNone(self.fixture.poll(), "the watchdog must never stop the inspected JVM")

    def test_progressing_tests_are_left_alone(self):
        output = self._run_watchdog(seconds=5, touch_progress=True)

        self.assertNotIn("::warning::", output)
        self.assertFalse(self.jcmd_calls.exists())

    def test_missing_progress_file_is_not_a_stall(self):
        self.progress.unlink()

        output = self._run_watchdog(seconds=5)

        self.assertNotIn("::warning::", output)
        self.assertFalse(self.jcmd_calls.exists())


if __name__ == "__main__":
    unittest.main()
