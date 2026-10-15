import json
import os
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HELPER = ROOT / ".scripts" / "with-linux-test-window-manager.sh"
BASH = shutil.which("bash")


class LinuxDesktopWindowManagerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="feedflow-wm-test-")
        self.work = Path(self.temp.name)
        self.bin = self.work / "bin"
        self.bin.mkdir()
        self.state = self.work / "state"
        self.state.mkdir()
        self.events = self.state / "events.jsonl"
        self.pid_file = self.state / "openbox.pid"
        self.xprop_calls = self.state / "xprop-calls"
        self.probe_file = self.state / "probes"
        self._write_executable(
            "openbox",
            f"""#!{sys.executable}
import json, os, signal, sys, time
state = os.environ['WM_TEST_STATE']
with open(os.path.join(state, 'events.jsonl'), 'a') as f:
    f.write(json.dumps({{'event': 'start', 'pid': os.getpid(), 'args': sys.argv[1:]}}) + '\\n')
with open(os.path.join(state, 'openbox.pid'), 'w') as f:
    f.write(str(os.getpid()))
def stop(signum, frame):
    with open(os.path.join(state, 'events.jsonl'), 'a') as f:
        f.write(json.dumps({{'event': 'stop', 'pid': os.getpid()}}) + '\\n')
    sys.exit(0)
signal.signal(signal.SIGTERM, stop)
if os.environ.get('WM_TEST_OPENBOX_MODE') == 'exit':
    sys.exit(23)
while True:
    time.sleep(0.02)
""",
        )
        self._write_executable(
            "xprop",
            f"""#!{sys.executable}
import os, pathlib, sys, time
state = pathlib.Path(os.environ['WM_TEST_STATE'])
calls_file = state / 'xprop-calls'
calls = int(calls_file.read_text() if calls_file.exists() else '0') + 1
calls_file.write_text(str(calls))
if os.environ.get('WM_TEST_PREEXISTING') == '1':
    print('_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x55')
elif calls > 1:
    probe = state / 'probes'
    count = int(probe.read_text() if probe.exists() else '0') + 1
    probe.write_text(str(count))
    for _ in range(100):
        if (state / 'openbox.pid').exists():
            break
        time.sleep(0.001)
    if not (state / 'openbox.pid').exists():
        sys.exit(0)
    if os.environ.get('WM_TEST_NEVER_READY') == '1' or count < 2:
        sys.exit(0)
    pid = int((state / 'openbox.pid').read_text())
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        pass
    else:
        print('_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x55')
""",
        )
        self._write_executable("sleep", "#!/bin/sh\nexit 0\n")
        self._write_executable(
            "mkdir",
            f"""#!{sys.executable}
import os, sys
args = sys.argv[1:]
if args and args[0] == '-p':
    args = args[1:]
for path in args:
    os.makedirs(path, exist_ok=True)
""",
        )
        self.log_dir = self.work / "desktopApp" / "build" / "reports" / "tests"

    def tearDown(self):
        self._stop_recorded_process()
        self.temp.cleanup()

    def _write_executable(self, name, body):
        path = self.bin / name
        path.write_text(body)
        path.chmod(0o755)
        return path

    def _env(self, **extra):
        env = os.environ.copy()
        env.update(
            PATH=str(self.bin),
            DISPLAY=":91",
            WM_TEST_STATE=str(self.state),
            WM_TEST_OPENBOX_MODE="ready",
        )
        env.update({key: str(value) for key, value in extra.items()})
        return env

    def _run(self, args=(), env=None, timeout=8):
        self.assertIsNotNone(BASH, "bash must be available to run the wrapper")
        return subprocess.run(
            [BASH, str(HELPER), *args],
            cwd=self.work,
            env=env or self._env(),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
        )

    def _events(self):
        if not self.events.exists():
            return []
        return [json.loads(line) for line in self.events.read_text().splitlines()]

    def _started_pid(self):
        starts = [event["pid"] for event in self._events() if event["event"] == "start"]
        return starts[0] if starts else None

    def _assert_stopped(self, pid):
        self.assertIsNotNone(pid, "fake openbox should have started")
        stops = [event["pid"] for event in self._events() if event["event"] == "stop"]
        self.assertIn(pid, stops, "wrapper should terminate its own window manager")
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def _stop_recorded_process(self):
        pid = self._started_pid()
        if pid is not None:
            try:
                os.kill(pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            deadline = time.monotonic() + 0.5
            while time.monotonic() < deadline:
                try:
                    os.kill(pid, 0)
                except ProcessLookupError:
                    break
                time.sleep(0.01)

    def test_requires_display_and_command(self):
        missing_display = self._env()
        missing_display.pop("DISPLAY")
        result = self._run((sys.executable, "-c", "pass"), missing_display)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self._events(), [])

        no_command = self._run((), self._env())
        self.assertNotEqual(no_command.returncode, 0)
        self.assertEqual(self._events(), [])

    def test_requires_openbox_and_xprop(self):
        for missing in ("openbox", "xprop"):
            with self.subTest(missing=missing):
                hidden = self.work / f"without-{missing}"
                hidden.mkdir()
                for executable in ("openbox", "xprop", "sleep"):
                    if executable != missing:
                        (hidden / executable).symlink_to(self.bin / executable)
                result = self._run(
                    (sys.executable, "-c", "pass"),
                    self._env(PATH=str(hidden)),
                )
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(self._events(), [])

    def test_refuses_preexisting_window_manager_without_launching_one(self):
        result = self._run(
            (sys.executable, "-c", "pass"), self._env(WM_TEST_PREEXISTING="1")
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self._events(), [])

    def test_waits_for_readiness_then_passes_arguments_verbatim_and_cleans_up(self):
        output = self.work / "arguments.json"
        args = ["with spaces", "$(touch should-not-exist)", "quote'and\"double"]
        code = "import json,sys,pathlib; pathlib.Path(sys.argv[1]).write_text(json.dumps(sys.argv[2:]))"
        result = self._run((sys.executable, "-c", code, str(output), *args))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(output.read_text()), args)
        self.assertFalse((self.work / "should-not-exist").exists())
        self.assertGreaterEqual(int(self.probe_file.read_text()), 2)
        pid = self._started_pid()
        self._assert_stopped(pid)
        logs = list(self.log_dir.glob("*")) if self.log_dir.exists() else []
        self.assertTrue(logs, "openbox output should be kept under test reports")

    def test_preserves_target_exit_status_and_stops_owned_manager(self):
        result = self._run((sys.executable, "-c", "raise SystemExit(37)"))
        self.assertEqual(result.returncode, 37, result.stderr)
        self._assert_stopped(self._started_pid())

    def test_fails_after_readiness_limit_and_stops_manager(self):
        marker = self.work / "target-ran"
        code = "import pathlib,sys; pathlib.Path(sys.argv[1]).write_text('yes')"
        result = self._run(
            (sys.executable, "-c", code, str(marker)),
            self._env(WM_TEST_NEVER_READY="1"),
            timeout=8,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(marker.exists(), "target must wait until the WM is ready")
        self.assertEqual(int(self.probe_file.read_text()), 100)
        self._assert_stopped(self._started_pid())

    def test_fails_if_manager_exits_before_readiness(self):
        marker = self.work / "target-ran"
        code = "import pathlib,sys; pathlib.Path(sys.argv[1]).write_text('yes')"
        result = self._run(
            (sys.executable, "-c", code, str(marker)),
            self._env(WM_TEST_OPENBOX_MODE="exit", WM_TEST_NEVER_READY="1"),
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(marker.exists(), "target must not run when the WM exits during startup")
        pid = self._started_pid()
        self.assertIsNotNone(pid, "the fake WM should start before exiting")
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def test_fails_if_manager_exits_during_target_command(self):
        script = self.work / "stop-manager.py"
        script.write_text(
            "import os, pathlib, signal, time; "
            "pid=int(pathlib.Path(os.environ['WM_TEST_STATE'], 'openbox.pid').read_text()); "
            "os.kill(pid, signal.SIGTERM); time.sleep(0.08)"
        )
        result = self._run((sys.executable, str(script)))
        self.assertNotEqual(result.returncode, 0)
        self._assert_stopped(self._started_pid())


if __name__ == "__main__":
    unittest.main()
