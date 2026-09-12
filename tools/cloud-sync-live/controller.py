#!/usr/bin/env python3
"""Optional physical-iPhone UI transport; never runs or replaces Gradle tests."""

import argparse
import contextlib
import fcntl
import json
import math
import re
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path


ACTIONS = (
    "inspect", "tap", "longPress", "typeText", "swipeUp", "swipeDown",
    "refresh", "activate", "background", "screenshot", "stop",
)


class ControllerError(Exception):
    pass


def write_json(path, value):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


def run_command(command, timeout):
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired as error:
        raise ControllerError(f"Command timed out after {timeout:g}s: {command[0]}") from error
    if result.returncode:
        diagnostic = (result.stderr or result.stdout).strip()
        raise ControllerError(f"Command failed ({result.returncode}): {diagnostic}")
    return result


def prepare(args):
    for name in ("app_bundle_id", "controller_bundle_id"):
        if not re.fullmatch(r"[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+", getattr(args, name)):
            raise ControllerError(f"Invalid {name.replace('_', '-')}")
    if not re.fullmatch(r"[A-Z0-9]{10}", args.team_id):
        raise ControllerError("team-id must be the ten-character Apple development team ID")
    if args.controller_bundle_id == args.app_bundle_id:
        raise ControllerError("The controller host must have its own bundle ID")
    if not shutil.which("xcodegen"):
        raise ControllerError("Install XcodeGen before preparing the controller")
    root = args.run_dir.resolve()
    # Never overwrite the configuration/evidence of a previous session.
    root.mkdir(parents=True, exist_ok=False)
    project = root / "ios-project"
    shutil.copytree(Path(__file__).parent / "ios", project)
    run_id = str(uuid.uuid4())
    test_bundle_id = args.controller_bundle_id + ".UITests"
    configuration = {
        "runID": run_id, "appBundleID": args.app_bundle_id,
        "timeoutSeconds": args.timeout_seconds,
    }
    write_json(project / "Configuration.json", configuration)
    template = (project / "project.yml.template").read_text()
    for key, value in {
        "__TEAM_ID__": args.team_id,
        "__HOST_BUNDLE_ID__": args.controller_bundle_id,
        "__TEST_BUNDLE_ID__": test_bundle_id,
    }.items():
        template = template.replace(key, value)
    (project / "project.yml").write_text(template)
    try:
        run_command(["xcodegen", "generate", "--spec", str(project / "project.yml")], 60)
    except ControllerError as error:
        raise ControllerError(f"{error}. Generated files remain in {root}; fix the cause and use a fresh run-dir") from error
    manifest = {
        "run_id": run_id, "device_id": args.device,
        "app_bundle_id": args.app_bundle_id,
        "runner_bundle_id": test_bundle_id + ".xctrunner",
        "project": str(project / "LiveCloudSyncHost.xcodeproj"),
        "scheme": "LiveCloudSyncHost",
    }
    write_json(root / "run.json", manifest)
    (root / "commands").mkdir()
    print(json.dumps(manifest, indent=2))


@contextlib.contextmanager
def session(root):
    manifest = json.loads((root / "run.json").read_text())
    with (root / "controller.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise ControllerError("Another command is active for this run; wait for it to finish") from error
        yield manifest


def transfer(manifest, direction, source, destination, timeout):
    # devicectl rejects fractional seconds, including a fractional remaining deadline.
    timeout = max(1, math.ceil(timeout))
    return run_command([
        "xcrun", "devicectl", "device", "copy", direction,
        "--device", manifest["device_id"],
        "--domain-type", "appDataContainer",
        "--domain-identifier", manifest["runner_bundle_id"],
        "--source", str(source), "--destination", str(destination),
        "--timeout", str(timeout),
    ], timeout + 2)


def collect(root, manifest, timeout):
    pending_path = root / "pending.json"
    if not pending_path.exists():
        raise ControllerError("No pending command to collect")
    pending = json.loads(pending_path.read_text())
    response_path = root / "commands" / (pending["id"] + ".response.json")
    incoming = root / "incoming.json"
    deadline = time.monotonic() + timeout
    last_error = "No matching response yet"
    while time.monotonic() < deadline:
        remaining = deadline - time.monotonic()
        try:
            transfer(manifest, "from", f"Documents/{manifest['run_id']}/response.json",
                     incoming, min(15, max(1, remaining)))
            response = json.loads(incoming.read_text())
            if response.get("id") != pending["id"] or response.get("runID") != manifest["run_id"]:
                last_error = "Runner returned a response for a different command/session"
            elif not isinstance(response.get("ok"), bool) or not isinstance(response.get("tree"), str):
                last_error = "Runner returned a malformed response"
            else:
                write_json(response_path, response)
                (response_path.with_suffix(".txt")).write_text(response["tree"] + "\n")
                if response.get("screenshot"):
                    expected = pending["id"] + ".png"
                    if response["screenshot"] != expected:
                        raise ControllerError("Unexpected screenshot filename in response")
                    transfer(manifest, "from", f"Documents/{manifest['run_id']}/{expected}",
                             root / "commands" / expected, min(15, max(1, deadline - time.monotonic())))
                pending_path.unlink()
                print(response["tree"])
                print(f"Response: {response_path}\nOK: {response['ok']}")
                if not response["ok"]:
                    raise ControllerError(response.get("error") or "UI action failed")
                return response
        except (ControllerError, json.JSONDecodeError) as error:
            if not pending_path.exists():
                raise
            last_error = str(error)
        time.sleep(min(0.5, max(0, deadline - time.monotonic())))
    raise ControllerError(
        f"No completed response within {timeout:g}s. {last_error}. "
        "The action may have run. Use collect; do not resend the mutation."
    )


def send(args, root, manifest):
    if (root / "pending.json").exists():
        raise ControllerError("A command is unresolved. Use collect or explicitly abandon it before sending another")
    if args.action in ("tap", "longPress") and not args.target:
        raise ControllerError(f"{args.action} requires --target (observed identifier or exact label)")
    if args.action == "typeText" and args.text is None:
        raise ControllerError("typeText requires --text; it types into the currently focused field")
    if args.action == "typeText" and args.target:
        raise ControllerError("typeText uses the current focus; tap the field separately before typing")
    command = {"id": str(uuid.uuid4()), "runID": manifest["run_id"], "action": args.action}
    if args.target:
        command["target"] = args.target
    if args.text is not None:
        command["text"] = args.text
    outgoing = root / "commands" / (command["id"] + ".command.json")
    write_json(outgoing, command)
    # Record uncertainty before dispatch: a transfer timeout may follow a completed UI action.
    write_json(root / "pending.json", command)
    try:
        transfer(manifest, "to", outgoing, f"Documents/{manifest['run_id']}/command.json",
                 min(20, args.timeout_seconds))
    except ControllerError as error:
        raise ControllerError(f"{error}. Command remains pending; use collect before any new action") from error
    collect(root, manifest, args.timeout_seconds)


def positive_seconds(value):
    seconds = int(value)
    if not 1 <= seconds <= 14400:
        raise argparse.ArgumentTypeError("Use a timeout between 1 and 14400 seconds")
    return seconds


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="operation", required=True)
    prepare_parser = subparsers.add_parser("prepare", help="Generate an isolated XcodeGen controller project; does not install/run")
    prepare_parser.add_argument("--run-dir", type=Path, required=True)
    prepare_parser.add_argument("--device", required=True, help="Explicit physical device UDID or CoreDevice ID")
    prepare_parser.add_argument("--app-bundle-id", required=True, help="Already installed FeedFlow dev app")
    prepare_parser.add_argument("--controller-bundle-id", required=True, help="Separate host bundle ID available to your signing team")
    prepare_parser.add_argument("--team-id", required=True)
    prepare_parser.add_argument("--timeout-seconds", type=positive_seconds, default=5400)
    for name in ("send", "collect", "abandon"):
        subparser = subparsers.add_parser(name)
        subparser.add_argument("--run-dir", type=Path, required=True)
        if name != "abandon":
            subparser.add_argument("--timeout-seconds", type=positive_seconds, default=60)
        if name == "send":
            subparser.add_argument("action", choices=ACTIONS)
            subparser.add_argument("--target")
            subparser.add_argument("--text")
    args = parser.parse_args(argv)
    try:
        if args.operation == "prepare":
            prepare(args)
        else:
            root = args.run_dir.resolve()
            with session(root) as manifest:
                if args.operation == "send":
                    send(args, root, manifest)
                elif args.operation == "collect":
                    collect(root, manifest, args.timeout_seconds)
                else:
                    pending = json.loads((root / "pending.json").read_text())
                    (root / "pending.json").replace(root / "commands" / (pending["id"] + ".abandoned.json"))
                    print("Abandoned unresolved command. Its action may have run; inspect the UI before any next mutation.")
    except (ControllerError, OSError, ValueError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
