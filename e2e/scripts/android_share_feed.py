"""Bridge Maestro's duplicate-share regression through a real ACTION_SEND intent."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time


_MARKER = "# NATIVE_ACTION_SEND: android_share_feed.py opens the real system resolver here.\n"
_HEADER = "appId: com.prof18.feedflow.debug\n---\n"
_URL = "https://www.androidcentral.com/feed"


def split_flow(source):
    if not source.startswith(_HEADER) or source.count(_MARKER) != 1:
        raise ValueError("expected the debug app header and exactly one native share marker")
    setup, result = source[len(_HEADER):].split(_MARKER)
    if "feedflow://e2e/reset-and-seed?profile=content-rich" not in setup:
        raise ValueError("the duplicate share requires the local content-rich seed")
    if '- assertVisible: "Android Central RSS Feed is already in your feeds."' not in result:
        raise ValueError("missing duplicate-result assertion")
    return _HEADER + setup, _HEADER + result


def run_journey(device, flow, output, *, run=subprocess.run):
    source = flow.read_text()
    setup, result_flow = split_flow(source)
    output.mkdir(parents=True, exist_ok=False)
    deadline = time.monotonic() + 180
    result = {"device": device, "sourceSha256": hashlib.sha256(source.encode()).hexdigest(),
              "passed": False, "phases": {}}

    def command(name, args):
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("share journey exceeded its three-minute limit")
        completed = run(args, stdin=subprocess.DEVNULL, capture_output=True, text=True,
                        timeout=min(60, remaining), check=False)
        (output / f"{name}.log").write_text(completed.stdout + completed.stderr)
        result["phases"][name] = completed.returncode
        if completed.returncode:
            raise RuntimeError(f"{name} failed with exit {completed.returncode}")

    def phase(name, body):
        path = output / f"{name}.yaml"
        path.write_text(body)
        args = ["maestro", f"--device={device}", "--platform", "android", "test"]
        if name == "result":
            args.append("--no-reinstall-driver")
        command(name, args + ["--format", "JUNIT", "--output", str(output / f"{name}.xml"),
                             "--test-output-dir", str(output / f"{name}-artifacts"), str(path)])

    try:
        command("release-layout-driver", ["adb", "-s", device, "shell", "am", "force-stop",
                                          "com.android.cli.interact.instrumentation"])
        phase("setup", setup)
        command("send", ["adb", "-s", device, "shell", "am", "start", "-W", "-a",
                         "android.intent.action.SEND", "-t", "text/plain", "--es",
                         "android.intent.extra.TEXT", _URL])
        phase("result", result_flow)
        result["passed"] = True
    except (OSError, RuntimeError, TimeoutError, subprocess.TimeoutExpired) as error:
        result["error"] = str(error)
    finally:
        (output / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True)
    parser.add_argument("--flow", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--parent-restores", action="store_true", required=True,
                        help="The calling runner must restore development feeds in its EXIT trap")
    args = parser.parse_args()
    result = run_journey(args.device, args.flow, args.output.resolve())
    print(json.dumps(result, indent=2))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
