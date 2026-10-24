#!/usr/bin/env python3
"""Run FeedFlow Maestro release tests and write Markdown and HTML reports."""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
import html
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


ANDROID_APP_ID = "com.prof18.feedflow.debug"
IOS_SIMULATOR_NAME = "iPhone 18 Pro"
SUITES = ("smoke", "regression")
COMMAND_TIMEOUT_RETURN_CODE = 124
MAESTRO_FLOW_TIMEOUT_SECONDS = 180
MAESTRO_MAX_ATTEMPTS = 3
ATC_LEASE_TTL_SECONDS = 900
ATC_RENEW_INTERVAL_SECONDS = 120
ATC_CLAIM_WAIT_SECONDS = 600
CALLER_LEASE_TTL_SECONDS = 3600
AUDIO_FLOWS = {
    "174-podcast-audio-playback.yaml",
    "175-now-playing-navigation.yaml",
    "176-reader-speech-playback.yaml",
}
# Flows that e2e/scripts/run-android.sh drives through a native helper instead of plain Maestro.
ANDROID_READER_TOOLTIP_FLOW = "e2e/maestro/android/smoke/007-reader-mode-core.yaml"
ANDROID_SHARE_FEED_FLOW = "e2e/maestro/android/regression/179-share-existing-feed.yaml"
PRINT_LOCK = threading.Lock()
ANSI_PATTERN = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")


@dataclass
class CommandResult:
    name: str
    returncode: int
    seconds: float
    log_path: Path


@dataclass
class FlowResult:
    platform: str
    suite: str
    flow_path: Path
    returncode: int
    seconds: float
    log_path: Path


@dataclass
class FailureAnalysis:
    classification: str
    reason: str
    evidence: str


@dataclass
class PlatformRunResult:
    platform: str
    setup_results: list[CommandResult]
    flow_results: list[FlowResult]
    restore_result: CommandResult | None = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run FeedFlow Maestro E2E release validation and generate HTML and Markdown reports."
    )
    parser.add_argument(
        "--platform",
        choices=("all", "android", "ios"),
        default="all",
        help="Platform suite to run. Defaults to all.",
    )
    parser.add_argument(
        "--suite",
        choices=("all", "smoke", "regression"),
        default="all",
        help="Suite to run. Defaults to all.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        help="Report directory. Defaults to .tmp/maestro-release-tests/<timestamp>.",
    )
    parser.add_argument(
        "--android-serial",
        help="Android device serial. Defaults to ANDROID_SERIAL, else the only connected device.",
    )
    parser.add_argument(
        "--ios-udid",
        help=f"iOS simulator UDID. Defaults to SIMULATOR_UDID, else the booted {IOS_SIMULATOR_NAME}.",
    )
    parser.add_argument(
        "--maestro-env",
        action="append",
        default=[],
        metavar="KEY=VALUE",
        help="Extra variable passed to every plain Maestro flow as -e KEY=VALUE. Repeatable.",
    )
    parser.add_argument(
        "--no-atc",
        action="store_true",
        help="Do not reserve the devices with atc, even when it is installed.",
    )
    return parser.parse_args()


def find_repo_root() -> Path:
    candidates = [Path.cwd(), *Path(__file__).resolve().parents]
    for candidate in candidates:
        if (candidate / "e2e/maestro").is_dir() and (candidate / "settings.gradle.kts").is_file():
            return candidate
    raise SystemExit("Could not find FeedFlow repo root from cwd or script location.")


def command_exists(name: str) -> bool:
    return shutil.which(name) is not None


def run_command(
    name: str,
    args: list[str],
    cwd: Path,
    log_path: Path,
    env: dict[str, str] | None = None,
    timeout_seconds: float | None = None,
) -> CommandResult:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    start = time.monotonic()
    print_line(f"\n==> {name}")
    print_line(f"    log: {log_path}")
    with log_path.open("w", encoding="utf-8") as log_file:
        log_file.write(f"$ {' '.join(args)}\n\n")
        process = subprocess.Popen(
            args,
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        assert process.stdout is not None
        def stream_output() -> None:
            assert process.stdout is not None
            for line in process.stdout:
                print_text(line)
                log_file.write(line)
                log_file.flush()

        output_thread = threading.Thread(target=stream_output, daemon=True)
        output_thread.start()
        timed_out = False
        try:
            returncode = process.wait(timeout=timeout_seconds)
        except subprocess.TimeoutExpired:
            timed_out = True
            print_line(f"    timed out after {timeout_seconds:.0f}s; terminating process")
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
            returncode = COMMAND_TIMEOUT_RETURN_CODE
        output_thread.join()
        if timed_out:
            log_file.write(f"\nCommand timed out after {timeout_seconds:.0f}s.\n")
    seconds = time.monotonic() - start
    print_line(f"<== {name}: exit {returncode} in {seconds:.1f}s")
    return CommandResult(name=name, returncode=returncode, seconds=seconds, log_path=log_path)


MAESTRO_INFRASTRUCTURE_TOKENS = (
    "deviceserverdiedexception",
    "device server died",
    "statusruntimeexception: unavailable",
    "deadline_exceeded",
    "command failed (tcp:",
    "deviceunreachableexception",
    "device became unreachable",
    "iosdrivertimeoutexception",
    "ios driver not ready in time",
)
DEBUG_OUTPUT_PATTERN = re.compile(r"^(/\S+/\.maestro/tests/\S+)\s*$", re.MULTILINE)


def maestro_debug_log_text(flow_log_text: str) -> str:
    """Maestro prints driver failures only to the maestro.log in its debug output directory."""
    texts = []
    for match in DEBUG_OUTPUT_PATTERN.finditer(ANSI_PATTERN.sub("", flow_log_text)):
        debug_log = Path(match.group(1)) / "maestro.log"
        if debug_log.is_file():
            texts.append(debug_log.read_text(encoding="utf-8", errors="replace"))
    return "\n".join(texts)


def has_infrastructure_failure(flow_log_text: str) -> bool:
    lower = (flow_log_text + "\n" + maestro_debug_log_text(flow_log_text)).lower()
    return any(token in lower for token in MAESTRO_INFRASTRUCTURE_TOKENS)


def retryable_maestro_failure(result: CommandResult) -> bool:
    if result.returncode == COMMAND_TIMEOUT_RETURN_CODE:
        return True
    return has_infrastructure_failure(read_log_text(result.log_path))


def run_maestro_command(
    name: str,
    args: list[str],
    cwd: Path,
    log_path: Path,
    env: dict[str, str] | None = None,
) -> CommandResult:
    total_seconds = 0.0
    for attempt in range(1, MAESTRO_MAX_ATTEMPTS + 1):
        result = run_command(
            name,
            args,
            cwd,
            log_path,
            env=env,
            timeout_seconds=MAESTRO_FLOW_TIMEOUT_SECONDS,
        )
        total_seconds += result.seconds
        if result.returncode == 0 or not retryable_maestro_failure(result):
            return CommandResult(name, result.returncode, total_seconds, log_path)
        if attempt == MAESTRO_MAX_ATTEMPTS:
            return CommandResult(name, result.returncode, total_seconds, log_path)

        archived_log = log_path.with_name(f"{log_path.stem}-attempt-{attempt}{log_path.suffix}")
        log_path.replace(archived_log)
        retry_delay = attempt * 5
        print_line(
            f"    retrying Maestro infrastructure failure in {retry_delay}s "
            f"(attempt {attempt + 1}/{MAESTRO_MAX_ATTEMPTS}; prior log: {archived_log})"
        )
        time.sleep(retry_delay)

    raise AssertionError("unreachable")


def print_line(message: str) -> None:
    with PRINT_LOCK:
        print(message, flush=True)


def print_text(message: str) -> None:
    with PRINT_LOCK:
        print(message, end="", flush=True)


def selected_platforms(platform_arg: str) -> tuple[str, ...]:
    if platform_arg == "all":
        return ("android", "ios")
    return (platform_arg,)


def selected_suites(suite_arg: str) -> tuple[str, ...]:
    if suite_arg == "all":
        return SUITES
    return (suite_arg,)


def preflight_failure(name: str, log_path: Path, message: str) -> CommandResult:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(message.rstrip() + "\n", encoding="utf-8")
    print_line(f"\n==> {name}\n    {message}")
    return CommandResult(name, 1, 0.0, log_path)


def capture(args: list[str], cwd: Path) -> tuple[int, str]:
    try:
        completed = subprocess.run(
            args, cwd=cwd, capture_output=True, text=True, timeout=60, check=False
        )
    except (OSError, subprocess.TimeoutExpired) as error:
        return 1, str(error)
    return completed.returncode, completed.stdout + completed.stderr


def resolve_android_serial(requested: str | None, repo_root: Path) -> tuple[str | None, str]:
    returncode, output = capture(["adb", "devices"], repo_root)
    if returncode != 0:
        return None, f"adb devices failed:\n{output}"
    online = [
        line.split()[0]
        for line in output.splitlines()[1:]
        if len(line.split()) >= 2 and line.split()[1] == "device"
    ]
    if requested:
        if requested not in online:
            return None, f"Android device {requested} is not connected and online. adb devices:\n{output}"
        return requested, ""
    if len(online) == 1:
        return online[0], ""
    if not online:
        return None, "No online Android device. Connect one or pass --android-serial."
    return None, (
        "Several Android devices are online; choose one with --android-serial or ANDROID_SERIAL: "
        + ", ".join(online)
    )


def resolve_ios_udid(requested: str | None, repo_root: Path) -> tuple[str | None, str]:
    returncode, output = capture(["xcrun", "simctl", "list", "devices", "booted"], repo_root)
    if returncode != 0:
        return None, f"xcrun simctl list failed:\n{output}"
    booted = {
        match.group(1): line.strip()
        for line in output.splitlines()
        if (match := re.search(r"\(([A-Fa-f0-9-]{36})\)", line))
    }
    if requested:
        if requested not in booted:
            return None, f"Simulator {requested} is not booted. Booted simulators:\n{output}"
        return requested, ""
    for udid, line in booted.items():
        if line.startswith(f"{IOS_SIMULATOR_NAME} ("):
            return udid, ""
    return None, f"No booted {IOS_SIMULATOR_NAME} simulator found. Boot it or pass --ios-udid."


class AtcLeases:
    """Reserves the test devices with atc so other agents and tools stay off them during the run.

    Leases the session already held before the run are reused and left in place; leases the
    runner creates are renewed while it runs and freed at the end.
    """

    def __init__(self, repo_root: Path, log_root: Path) -> None:
        self.repo_root = repo_root
        self.log_root = log_root
        self.targets: dict[str, str] = {}
        self.created: set[str] = set()
        self.stop_event = threading.Event()
        self.renew_thread: threading.Thread | None = None
        if not os.environ.get("ATC_SESSION_ID"):
            # Bind the leases to this process, so atc reclaims them if the runner dies.
            os.environ["ATC_SESSION_ID"] = f"maestro-release-{os.getpid()}-{int(time.time())}"
            os.environ["ATC_ANCHOR_PID"] = str(os.getpid())
        self.session_id = os.environ["ATC_SESSION_ID"]

    def session_holds(self, device_key: str) -> bool:
        returncode, output = capture(["atc", "status", "--json"], self.repo_root)
        if returncode != 0:
            return False
        try:
            leases = json.loads(output).get("leases", {})
        except json.JSONDecodeError:
            return False
        lease = leases.get(device_key)
        return bool(lease) and lease.get("sessionId") == self.session_id

    def claim(self, platform: str, target: str) -> CommandResult:
        if platform == "android":
            device_key = f"serial:{target}"
            args = ["atc", "claim", "--serial", target]
        else:
            device_key = f"sim:{target}"
            args = ["atc", "claim", "--ios", "--udid", target]
        already_held = self.session_holds(device_key)
        result = run_command(
            f"{platform} atc claim",
            [*args, "--ttl", str(ATC_LEASE_TTL_SECONDS), "--wait", str(ATC_CLAIM_WAIT_SECONDS)],
            self.repo_root,
            self.log_root / f"setup/{platform}/00-atc-claim.log",
        )
        if result.returncode == 0:
            self.targets[platform] = target
            if not already_held:
                self.created.add(platform)
            self.start_renewing()
        return result

    def start_renewing(self) -> None:
        if self.renew_thread is not None:
            return

        def renew_loop() -> None:
            while not self.stop_event.wait(ATC_RENEW_INTERVAL_SECONDS):
                for platform, target in list(self.targets.items()):
                    returncode, output = capture(
                        ["atc", "renew", target, "--ttl", str(ATC_LEASE_TTL_SECONDS)], self.repo_root
                    )
                    if returncode != 0:
                        print_line(f"Warning: atc renew for {platform} {target} failed: {output.strip()}")

        self.renew_thread = threading.Thread(target=renew_loop, daemon=True)
        self.renew_thread.start()

    def release(self) -> None:
        self.stop_event.set()
        for platform, target in self.targets.items():
            if platform not in self.created:
                # The run's renewals shortened the caller's lease; hand it back with room to spare.
                capture(["atc", "renew", target, "--ttl", str(CALLER_LEASE_TTL_SECONDS)], self.repo_root)
        for platform in sorted(self.created):
            target = self.targets[platform]
            returncode, output = capture(["atc", "free", target], self.repo_root)
            if returncode != 0:
                print_line(f"Warning: atc free for {platform} {target} failed: {output.strip()}")
            else:
                print_line(f"Released atc lease for {platform} {target}.")


def write_android_maestro_shim(log_root: Path, serial: str) -> Path:
    """Binds Maestro calls without an explicit device (restore utility, helpers) to the serial."""
    tools = log_root / "tools/android"
    tools.mkdir(parents=True, exist_ok=True)
    maestro_binary = shutil.which("maestro")
    shim = tools / "maestro"
    shim.write_text(
        "#!/usr/bin/env bash\n"
        "set -euo pipefail\n"
        "for argument in \"$@\"; do\n"
        "  case \"$argument\" in\n"
        f"    --device|--udid|--device=*|--udid=*) exec {shlex_quote(str(maestro_binary))} \"$@\" ;;\n"
        "  esac\n"
        "done\n"
        f"exec {shlex_quote(str(maestro_binary))} --device={shlex_quote(serial)} \"$@\"\n",
        encoding="utf-8",
    )
    shim.chmod(0o755)
    return tools


def shlex_quote(value: str) -> str:
    return "'" + value.replace("'", "'\"'\"'") + "'"


def setup_android(repo_root: Path, log_root: Path, env: dict[str, str]) -> list[CommandResult]:
    results: list[CommandResult] = []
    serial = env["ANDROID_SERIAL"]
    apk_path = repo_root / "androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"

    results.append(
        run_command(
            "android build",
            ["./gradlew", "--quiet", "--console=plain", ":androidApp:assembleGooglePlayDebug"],
            repo_root,
            log_root / "setup/android/01-build.log",
            env=env,
        )
    )
    if results[-1].returncode != 0:
        return results

    if command_exists("android"):
        install_args = ["android", "run", f"--device={serial}", f"--apks={apk_path}"]
    else:
        install_args = ["adb", "-s", serial, "install", "-r", str(apk_path)]
    results.append(
        run_command(
            "android install",
            install_args,
            repo_root,
            log_root / "setup/android/02-install.log",
            env=env,
        )
    )
    if results[-1].returncode != 0:
        return results

    if not command_exists("android"):
        results.append(
            run_command(
                "android launch",
                ["adb", "-s", serial, "shell", "monkey", "-p", ANDROID_APP_ID, "1"],
                repo_root,
                log_root / "setup/android/03-launch.log",
                env=env,
            )
        )
        if results[-1].returncode != 0:
            return results

    results.append(
        run_command(
            "android fixtures",
            [str(repo_root / "e2e/scripts/push-android-fixtures.sh")],
            repo_root,
            log_root / "setup/android/04-fixtures.log",
            env=env,
        )
    )
    return results


def setup_ios(repo_root: Path, log_root: Path, env: dict[str, str]) -> list[CommandResult]:
    results: list[CommandResult] = []
    simulator_udid = env["SIMULATOR_UDID"]
    xcodeproj = repo_root / "iosApp/FeedFlow.xcodeproj"
    if not xcodeproj.is_dir():
        results.append(
            run_command(
                "ios generate project",
                ["./.scripts/generate-project.sh"],
                repo_root / "iosApp",
                log_root / "setup/ios/01-generate-project.log",
                env=env,
            )
        )
        if results[-1].returncode != 0:
            return results

    derived_data_path = repo_root / "iosApp/build/e2e-derived-data"
    app_path = derived_data_path / "Build/Products/Debug-iphonesimulator/FeedFlow.app"
    results.append(
        run_command(
            "ios build",
            [
                "xcodebuild",
                "-project",
                "iosApp/FeedFlow.xcodeproj",
                "-scheme",
                "FeedFlow",
                "-destination",
                f"platform=iOS Simulator,id={simulator_udid}",
                "-derivedDataPath",
                str(derived_data_path),
                "build",
                "-quiet",
            ],
            repo_root,
            log_root / "setup/ios/02-build.log",
            env=env,
        )
    )
    if results[-1].returncode != 0:
        return results

    results.append(
        run_command(
            "ios install",
            ["xcrun", "simctl", "install", simulator_udid, str(app_path)],
            repo_root,
            log_root / "setup/ios/03-install.log",
            env=env,
        )
    )
    if results[-1].returncode != 0:
        return results

    results.append(
        run_command(
            "ios fixtures",
            [str(repo_root / "e2e/scripts/push-ios-fixtures.sh")],
            repo_root,
            log_root / "setup/ios/04-fixtures.log",
            env=env,
        )
    )
    return results


def flow_files(repo_root: Path, platform: str, suite: str) -> list[Path]:
    suite_dir = repo_root / "e2e/maestro" / platform / suite
    return sorted(suite_dir.glob("*.yaml"))


def safe_log_name(flow_path: Path) -> str:
    return flow_path.stem.replace("/", "_") + ".log"


def maestro_env_args(maestro_env: list[str]) -> list[str]:
    args: list[str] = []
    for assignment in maestro_env:
        args.extend(["-e", assignment])
    return args


def flow_command(
    platform: str,
    relative_flow: Path,
    env: dict[str, str],
    log_root: Path,
    maestro_env: list[str],
) -> list[str]:
    """Runs each flow the way e2e/scripts/run-android.sh and run-ios.sh do."""
    if relative_flow.name in AUDIO_FLOWS:
        return ["bash", "e2e/scripts/run-audio-playback.sh", platform, relative_flow.stem]
    if platform == "android":
        serial = env["ANDROID_SERIAL"]
        helper_output = log_root / "helpers/android" / relative_flow.stem
        if str(relative_flow) == ANDROID_READER_TOOLTIP_FLOW:
            return [
                "python3", "e2e/scripts/android_reader_tooltip.py", "--device", serial,
                "--flow", str(relative_flow), "--output", str(helper_output), "--parent-restores",
            ]
        if str(relative_flow) == ANDROID_SHARE_FEED_FLOW:
            return [
                "python3", "e2e/scripts/android_share_feed.py", "--device", serial,
                "--flow", str(relative_flow), "--output", str(helper_output), "--parent-restores",
            ]
        return [
            "maestro", "--device", serial, "--platform", "android", "test",
            *maestro_env_args(maestro_env), str(relative_flow),
        ]
    return [
        "maestro", "--platform", "ios", "--device", env["SIMULATOR_UDID"], "test",
        *maestro_env_args(maestro_env), str(relative_flow),
    ]


def run_flows(
    repo_root: Path,
    log_root: Path,
    platform: str,
    suites: tuple[str, ...],
    env: dict[str, str],
    maestro_env: list[str],
) -> list[FlowResult]:
    results: list[FlowResult] = []
    for suite in suites:
        flows = flow_files(repo_root, platform, suite)
        print_line(f"\n==> {platform} {suite}: {len(flows)} flows")
        for flow_path in flows:
            relative_flow = flow_path.relative_to(repo_root)
            log_path = log_root / "flows" / platform / suite / safe_log_name(flow_path)
            if platform == "ios":
                reset_flow = Path("e2e/maestro/ios/helpers/reset-orientation.yaml")
                reset_result = run_maestro_command(
                    f"{platform} {suite} {flow_path.name} orientation reset",
                    [
                        "maestro",
                        "--platform",
                        "ios",
                        "--device",
                        env["SIMULATOR_UDID"],
                        "test",
                        str(reset_flow),
                    ],
                    repo_root,
                    log_path.with_name(f"{flow_path.stem}-orientation-reset.log"),
                    env=env,
                )
                if reset_result.returncode != 0:
                    results.append(
                        FlowResult(
                            platform=platform,
                            suite=suite,
                            flow_path=relative_flow,
                            returncode=reset_result.returncode,
                            seconds=reset_result.seconds,
                            log_path=reset_result.log_path,
                        )
                    )
                    continue
            command_result = run_maestro_command(
                f"{platform} {suite} {flow_path.name}",
                flow_command(platform, relative_flow, env, log_root, maestro_env),
                repo_root,
                log_path,
                env=env,
            )
            results.append(
                FlowResult(
                    platform=platform,
                    suite=suite,
                    flow_path=relative_flow,
                    returncode=command_result.returncode,
                    seconds=command_result.seconds,
                    log_path=command_result.log_path,
                )
            )
    return results


def setup_failed(results: list[CommandResult]) -> bool:
    return any(result.returncode != 0 for result in results)


def prepare_platform(
    repo_root: Path,
    output_dir: Path,
    platform: str,
    env: dict[str, str],
    requested_target: str | None,
    leases: AtcLeases | None,
) -> list[CommandResult]:
    """Resolves the device, reserves it and binds every later command to it."""
    log_path = output_dir / f"setup/{platform}/00-device.log"
    if platform == "android":
        target, problem = resolve_android_serial(requested_target, repo_root)
    else:
        target, problem = resolve_ios_udid(requested_target, repo_root)
    if not target:
        return [preflight_failure(f"{platform} device preflight", log_path, problem)]

    results: list[CommandResult] = []
    if leases is not None:
        results.append(leases.claim(platform, target))
        if results[-1].returncode != 0:
            return results

    if platform == "android":
        env["ANDROID_SERIAL"] = target
        tools = write_android_maestro_shim(output_dir, target)
        env["PATH"] = f"{tools}{os.pathsep}{env.get('PATH', '')}"
    else:
        env["SIMULATOR_UDID"] = target
    print_line(f"{platform} device: {target}")
    return results


def run_platform_tests(
    repo_root: Path,
    output_dir: Path,
    platform: str,
    suites: tuple[str, ...],
    base_env: dict[str, str],
    requested_target: str | None,
    leases: AtcLeases | None,
    maestro_env: list[str],
) -> PlatformRunResult:
    env = base_env.copy()
    setup_results = prepare_platform(repo_root, output_dir, platform, env, requested_target, leases)
    if not setup_failed(setup_results):
        if platform == "android":
            setup_results.extend(setup_android(repo_root, output_dir, env))
        else:
            setup_results.extend(setup_ios(repo_root, output_dir, env))

    if setup_failed(setup_results):
        print_line(f"Skipping {platform} flows because setup failed.")
        return PlatformRunResult(platform, setup_results, [])

    flow_results = run_flows(repo_root, output_dir, platform, suites, env, maestro_env)
    restore_result: CommandResult | None = None
    if command_exists("feedflow-restore-dev-feeds"):
        restore_args = ["feedflow-restore-dev-feeds", "--platform", platform]
        if platform == "ios":
            restore_args.extend(["--simulator", env["SIMULATOR_UDID"]])
        restore_result = run_command(
            f"{platform} development-feed restore",
            restore_args,
            repo_root,
            output_dir / f"restore/{platform}.log",
            env=env,
        )
        if restore_result.returncode != 0:
            print_line(
                f"Warning: {platform} development-feed restore failed; "
                f"see {restore_result.log_path}."
            )
    else:
        print_line(
            f"Warning: feedflow-restore-dev-feeds is not installed; restore the {platform} "
            "development feeds manually."
        )

    return PlatformRunResult(
        platform,
        setup_results,
        flow_results,
        restore_result,
    )


def report_link(report_path: Path, target_path: Path) -> str:
    relative = os.path.relpath(target_path, start=report_path.parent)
    return html.escape(relative)


def short_text(value: str, max_length: int = 220) -> str:
    compact = " ".join(value.split())
    if len(compact) <= max_length:
        return compact
    return compact[: max_length - 1].rstrip() + "..."


def markdown_cell(value: str) -> str:
    return short_text(value).replace("|", "\\|").replace("\n", "<br>")


def clean_log_line(line: str) -> str:
    return ANSI_PATTERN.sub("", line).strip()


def read_log_text(log_path: Path) -> str:
    return log_path.read_text(encoding="utf-8", errors="replace")


def first_matching_line(text: str, patterns: tuple[str, ...]) -> str:
    for line in text.splitlines():
        clean_line = clean_log_line(line)
        lower_line = clean_line.lower()
        if clean_line and any(pattern in lower_line for pattern in patterns):
            return clean_line
    return ""


def last_meaningful_line(text: str) -> str:
    ignored_prefixes = ("$", "====")
    for line in reversed(text.splitlines()):
        clean_line = clean_log_line(line)
        if clean_line and not clean_line.startswith(ignored_prefixes):
            return clean_line
    return ""


def failure_evidence(text: str) -> str:
    salient_patterns = (
        "command timed out",
        "device server died",
        "assertion is false",
        "assertion '",
        "element selector",
        "failed to",
        "timed out",
        "timeout",
        "no devices",
        "device not found",
        "unable to",
        "not installed",
        "crashed",
        "fatal exception",
        "permission",
        "no such file",
    )
    return first_matching_line(text, salient_patterns) or last_meaningful_line(text)


def analyze_failure(log_path: Path, returncode: int, context: str) -> FailureAnalysis:
    if returncode == 0:
        return FailureAnalysis("Passed", "No failure detected.", "")

    text = read_log_text(log_path)
    lower = text.lower()
    evidence = failure_evidence(text)

    if (
        returncode == COMMAND_TIMEOUT_RETURN_CODE
        or "command timed out" in lower
        or has_infrastructure_failure(text)
    ):
        return FailureAnalysis(
            "Maestro infrastructure / driver",
            "The Maestro device driver or transport failed before the app flow could complete.",
            evidence,
        )

    if any(token in lower for token in ("fatal exception", "process crashed", "sigabrt", "anr in ")):
        return FailureAnalysis(
            "App crash / likely app issue",
            "The app process crashed or became unresponsive while the flow was running.",
            evidence,
        )

    if any(
        token in lower
        for token in (
            "no devices",
            "device not found",
            "could not find device",
            "no booted",
            "adb server",
            "xcrun",
        )
    ):
        return FailureAnalysis(
            "Environment / device setup",
            "The failure points to the test device, simulator, or host tooling instead of app behavior.",
            evidence,
        )

    if any(
        token in lower
        for token in (
            "not installed",
            "unable to launch app",
            "could not launch",
            "activity not started",
            "install_failed",
            "app id",
        )
    ):
        return FailureAnalysis(
            "Install / launch misconfiguration",
            "The app could not be installed or launched with the expected package, scheme, or activity.",
            evidence,
        )

    if any(token in lower for token in ("permission", "allow notifications", "access photos", "access files")):
        return FailureAnalysis(
            "System permission dialog",
            "A platform permission or system dialog likely interrupted the scripted path.",
            evidence,
        )

    if any(token in lower for token in ("documentsui", "file picker", "fixture", "no such file", "does not exist")):
        return FailureAnalysis(
            "Fixture / picker setup",
            "The flow likely missed a seeded fixture, file picker item, or expected document-provider state.",
            evidence,
        )

    if any(token in lower for token in ("scrolluntilvisible", "scrolling", "timed out", "timeout", "wait")):
        return FailureAnalysis(
            "Timing or scroll target",
            "The flow waited for an element or scroll position that was not reached in time; check for flakiness, test data drift, or a real missing row.",
            evidence,
        )

    if any(token in lower for token in ("assertion is false", "assertion '", "element selector may be incorrect")):
        return FailureAnalysis(
            "Expected UI state missing",
            "The flow reached an assertion but the expected text or selector was not visible; this is either app regression or test drift, depending on the screenshot state.",
            evidence,
        )

    if any(token in lower for token in ("tap on", "failed to tap", "element not found")):
        return FailureAnalysis(
            "Interaction target missing",
            "Maestro could not interact with the requested control; the selector may be stale or the UI may have changed.",
            evidence,
        )

    return FailureAnalysis(
        "Unknown / needs artifact review",
        f"The {context} exited non-zero, but the log did not match a known failure pattern.",
        evidence,
    )


def write_report(
    report_path: Path,
    setup_results: dict[str, list[CommandResult]],
    flow_results: list[FlowResult],
    platforms: tuple[str, ...],
    suites: tuple[str, ...],
) -> None:
    lines: list[str] = []
    failed_setup = [
        (platform, result)
        for platform, results in setup_results.items()
        for result in results
        if result.returncode != 0
    ]
    failed_flows = [result for result in flow_results if result.returncode != 0]
    passed_flows = [result for result in flow_results if result.returncode == 0]
    status = "FAILED" if failed_setup or failed_flows else "PASSED"

    lines.append(f"# Maestro Release Test Report")
    lines.append("")
    lines.append(f"- Status: {status}")
    lines.append(f"- Generated: {datetime.now().isoformat(timespec='seconds')}")
    lines.append(f"- Platforms: {', '.join(platforms)}")
    lines.append(f"- Suites: {', '.join(suites)}")
    lines.append(f"- Flow results: {len(passed_flows)} passed, {len(failed_flows)} failed")
    lines.append(f"- Log directory: `{report_path.parent}`")
    lines.append("")

    lines.append("## Summary")
    lines.append("")
    lines.append("| Platform | Suite | Passed | Failed | Total |")
    lines.append("| --- | --- | ---: | ---: | ---: |")
    for platform in platforms:
        for suite in suites:
            matching = [
                result
                for result in flow_results
                if result.platform == platform and result.suite == suite
            ]
            passed = sum(1 for result in matching if result.returncode == 0)
            failed = sum(1 for result in matching if result.returncode != 0)
            lines.append(f"| {platform} | {suite} | {passed} | {failed} | {len(matching)} |")
    lines.append("")

    if failed_setup:
        lines.append("## Setup Failures")
        lines.append("")
        lines.append("| Platform | Step | Exit | Classification | Brief analysis | Evidence | Log |")
        lines.append("| --- | --- | ---: | --- | --- | --- | --- |")
        for platform, result in failed_setup:
            analysis = analyze_failure(result.log_path, result.returncode, result.name)
            lines.append(
                f"| {platform} | {result.name} | {result.returncode} | "
                f"{markdown_cell(analysis.classification)} | {markdown_cell(analysis.reason)} | "
                f"{markdown_cell(analysis.evidence)} | `{result.log_path}` |"
            )
        lines.append("")

    if failed_flows:
        lines.append("## Failed Flows")
        lines.append("")
        lines.append("| Platform | Suite | Flow | Exit | Seconds | Classification | Brief analysis | Evidence | Log |")
        lines.append("| --- | --- | --- | ---: | ---: | --- | --- | --- | --- |")
        for result in failed_flows:
            analysis = analyze_failure(result.log_path, result.returncode, str(result.flow_path))
            lines.append(
                f"| {result.platform} | {result.suite} | `{result.flow_path}` | "
                f"{result.returncode} | {result.seconds:.1f} | "
                f"{markdown_cell(analysis.classification)} | {markdown_cell(analysis.reason)} | "
                f"{markdown_cell(analysis.evidence)} | `{result.log_path}` |"
            )
        lines.append("")

    lines.append("## All Flow Results")
    lines.append("")
    lines.append("| Platform | Suite | Flow | Result | Seconds | Analysis | Log |")
    lines.append("| --- | --- | --- | --- | ---: | --- | --- |")
    for result in flow_results:
        outcome = "PASS" if result.returncode == 0 else "FAIL"
        analysis = analyze_failure(result.log_path, result.returncode, str(result.flow_path))
        analysis_text = "-" if result.returncode == 0 else analysis.classification
        lines.append(
            f"| {result.platform} | {result.suite} | `{result.flow_path}` | {outcome} | "
            f"{result.seconds:.1f} | {markdown_cell(analysis_text)} | `{result.log_path}` |"
        )
    lines.append("")

    report_path.write_text("\n".join(lines), encoding="utf-8")


def setup_status(setup_results: dict[str, list[CommandResult]], platform: str) -> str:
    results = setup_results.get(platform, [])
    if not results:
        return "not run"
    return "failed" if setup_failed(results) else "passed"


def suite_summary_rows(
    flow_results: list[FlowResult],
    platforms: tuple[str, ...],
    suites: tuple[str, ...],
) -> list[tuple[str, str, int, int, int]]:
    rows = []
    for platform in platforms:
        for suite in suites:
            matching = [
                result
                for result in flow_results
                if result.platform == platform and result.suite == suite
            ]
            passed = sum(1 for result in matching if result.returncode == 0)
            failed = sum(1 for result in matching if result.returncode != 0)
            rows.append((platform, suite, passed, failed, len(matching)))
    return rows


def result_class(returncode: int | None) -> str:
    if returncode is None:
        return "not-run"
    return "passed" if returncode == 0 else "failed"


def result_label(returncode: int | None) -> str:
    if returncode is None:
        return "NOT RUN"
    return "PASS" if returncode == 0 else "FAIL"


def write_html_report(
    report_path: Path,
    repo_root: Path,
    setup_results: dict[str, list[CommandResult]],
    flow_results: list[FlowResult],
    platforms: tuple[str, ...],
    suites: tuple[str, ...],
) -> None:
    failed_setup = [
        (platform, result)
        for platform, results in setup_results.items()
        for result in results
        if result.returncode != 0
    ]
    failed_flows = [result for result in flow_results if result.returncode != 0]
    passed_flows = [result for result in flow_results if result.returncode == 0]
    total_planned = sum(
        len(flow_files(repo_root, platform, suite))
        for platform in platforms
        for suite in suites
    )
    status = "FAILED" if failed_setup or failed_flows else "PASSED"
    generated = datetime.now().isoformat(timespec="seconds")
    duration = sum(result.seconds for result in flow_results)
    flow_result_by_key = {
        (result.platform, result.suite, str(result.flow_path)): result for result in flow_results
    }

    lines: list[str] = [
        "<!doctype html>",
        '<html lang="en">',
        "<head>",
        '<meta charset="utf-8">',
        '<meta name="viewport" content="width=device-width, initial-scale=1">',
        "<title>FeedFlow Maestro Release Report</title>",
        "<style>",
        ":root {",
        "  --bg: #f6f1e8;",
        "  --paper: #fffdf8;",
        "  --ink: #19231d;",
        "  --muted: #68746b;",
        "  --line: #ded5c5;",
        "  --pass: #177245;",
        "  --pass-bg: #dff2e8;",
        "  --fail: #b5252a;",
        "  --fail-bg: #f8dddd;",
        "  --skip: #6e6252;",
        "  --skip-bg: #eee6d8;",
        "  --accent: #1d5b79;",
        "  --accent-bg: #dcebf1;",
        "}",
        "* { box-sizing: border-box; }",
        "body { margin: 0; background: radial-gradient(circle at top left, #dcebf1 0, transparent 28rem), var(--bg); color: var(--ink); font: 15px/1.45 Charter, Georgia, serif; }",
        "main { width: min(1180px, calc(100vw - 32px)); margin: 0 auto; padding: 32px 0 56px; }",
        "header { border-bottom: 3px solid var(--ink); padding: 26px 0 20px; margin-bottom: 24px; }",
        "h1 { font-size: clamp(32px, 5vw, 58px); line-height: 0.95; margin: 0 0 14px; letter-spacing: 0; }",
        "h2 { font-size: 24px; margin: 34px 0 12px; letter-spacing: 0; }",
        "h3 { font-size: 18px; margin: 18px 0 8px; letter-spacing: 0; }",
        ".meta { display: flex; flex-wrap: wrap; gap: 10px; color: var(--muted); }",
        ".pill { display: inline-flex; align-items: center; border: 1px solid var(--line); border-radius: 999px; padding: 5px 10px; background: rgba(255,255,255,0.55); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }",
        ".pill.status-passed { background: var(--pass-bg); border-color: #b7dfc9; color: var(--pass); }",
        ".pill.status-failed { background: var(--fail-bg); border-color: #edb8ba; color: var(--fail); }",
        ".cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }",
        ".card { background: var(--paper); border: 1px solid var(--line); border-radius: 8px; padding: 16px; box-shadow: 0 10px 26px rgba(33, 28, 20, 0.07); }",
        ".card strong { display: block; font-size: 30px; line-height: 1; margin-bottom: 6px; }",
        ".card span { color: var(--muted); font-size: 13px; }",
        ".split { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, 0.6fr); gap: 18px; align-items: start; }",
        "table { width: 100%; border-collapse: collapse; background: var(--paper); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }",
        "th, td { padding: 10px 12px; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }",
        "th { font: 12px/1.2 ui-monospace, SFMono-Regular, Menlo, monospace; text-transform: uppercase; color: var(--muted); background: #f1eadf; }",
        "tr:last-child td { border-bottom: 0; }",
        "code, .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }",
        "a { color: var(--accent); text-decoration-thickness: 2px; }",
        ".badge { display: inline-flex; min-width: 72px; justify-content: center; border-radius: 999px; padding: 4px 8px; font: 12px/1 ui-monospace, SFMono-Regular, Menlo, monospace; }",
        ".badge.passed { color: var(--pass); background: var(--pass-bg); }",
        ".badge.failed { color: var(--fail); background: var(--fail-bg); }",
        ".badge.not-run { color: var(--skip); background: var(--skip-bg); }",
        ".analysis { max-width: 340px; }",
        ".evidence { color: var(--muted); max-width: 360px; }",
        ".empty { color: var(--muted); background: var(--paper); border: 1px dashed var(--line); border-radius: 8px; padding: 16px; }",
        "details { background: var(--paper); border: 1px solid var(--line); border-radius: 8px; margin: 10px 0; overflow: hidden; }",
        "summary { cursor: pointer; padding: 12px 14px; font-weight: 700; }",
        "details table { border: 0; border-top: 1px solid var(--line); border-radius: 0; }",
        "@media (max-width: 860px) { .cards, .split { grid-template-columns: 1fr; } th, td { padding: 8px; } }",
        "</style>",
        "</head>",
        "<body>",
        "<main>",
        "<header>",
        "<h1>FeedFlow Maestro Release Report</h1>",
        '<div class="meta">',
        f'<span class="pill status-{status.lower()}">Status: {status}</span>',
        f'<span class="pill">Generated: {html.escape(generated)}</span>',
        f'<span class="pill">Platforms: {html.escape(", ".join(platforms))}</span>',
        f'<span class="pill">Suites: {html.escape(", ".join(suites))}</span>',
        f'<span class="pill">Repo: {html.escape(str(repo_root))}</span>',
        "</div>",
        "</header>",
        '<section class="cards" aria-label="Run summary">',
        f'<div class="card"><strong>{len(passed_flows)}</strong><span>Flows passed</span></div>',
        f'<div class="card"><strong>{len(failed_flows)}</strong><span>Flows failed</span></div>',
        f'<div class="card"><strong>{total_planned}</strong><span>Flows in selected suites</span></div>',
        f'<div class="card"><strong>{duration / 60:.1f}m</strong><span>Flow runtime only</span></div>',
        "</section>",
    ]

    lines.extend(
        [
            '<section class="split">',
            "<div>",
            "<h2>Suite Summary</h2>",
            "<table>",
            "<thead><tr><th>Platform</th><th>Suite</th><th>Passed</th><th>Failed</th><th>Ran</th></tr></thead>",
            "<tbody>",
        ]
    )
    for platform, suite, passed, failed, total in suite_summary_rows(flow_results, platforms, suites):
        lines.append(
            "<tr>"
            f"<td>{html.escape(platform)}</td>"
            f"<td>{html.escape(suite)}</td>"
            f"<td>{passed}</td>"
            f"<td>{failed}</td>"
            f"<td>{total}</td>"
            "</tr>"
        )
    lines.extend(["</tbody>", "</table>", "</div>", "<div>", "<h2>Setup</h2>"])
    for platform in platforms:
        platform_status = setup_status(setup_results, platform)
        lines.append(
            f'<details open><summary>{html.escape(platform.title())} setup '
            f'<span class="badge {result_class(0 if platform_status == "passed" else None if platform_status == "not run" else 1)}">'
            f"{html.escape(platform_status.upper())}</span></summary>"
        )
        lines.append(
            "<table><thead><tr><th>Step</th><th>Exit</th><th>Seconds</th>"
            "<th>Classification</th><th>Brief analysis</th><th>Evidence</th><th>Log</th></tr></thead><tbody>"
        )
        for result in setup_results.get(platform, []):
            analysis = analyze_failure(result.log_path, result.returncode, result.name)
            classification = "-" if result.returncode == 0 else html.escape(short_text(analysis.classification))
            reason = "-" if result.returncode == 0 else html.escape(short_text(analysis.reason))
            evidence = "-" if result.returncode == 0 else html.escape(short_text(analysis.evidence))
            lines.append(
                "<tr>"
                f"<td>{html.escape(result.name)}</td>"
                f'<td><span class="badge {result_class(result.returncode)}">{result.returncode}</span></td>'
                f"<td>{result.seconds:.1f}</td>"
                f'<td class="analysis">{classification}</td>'
                f'<td class="analysis">{reason}</td>'
                f'<td class="evidence"><code>{evidence}</code></td>'
                f'<td><a href="{report_link(report_path, result.log_path)}">log</a></td>'
                "</tr>"
            )
        if not setup_results.get(platform):
            lines.append('<tr><td colspan="7" class="empty">Setup did not run.</td></tr>')
        lines.extend(["</tbody></table>", "</details>"])
    lines.extend(["</div>", "</section>"])

    lines.append("<section>")
    lines.append("<h2>Failed Flows</h2>")
    if failed_flows:
        lines.append("<table>")
        lines.append(
            "<thead><tr><th>Platform</th><th>Suite</th><th>Flow</th><th>Exit</th><th>Seconds</th>"
            "<th>Classification</th><th>Brief analysis</th><th>Evidence</th><th>Log</th></tr></thead>"
        )
        lines.append("<tbody>")
        for result in failed_flows:
            analysis = analyze_failure(result.log_path, result.returncode, str(result.flow_path))
            lines.append(
                "<tr>"
                f"<td>{html.escape(result.platform)}</td>"
                f"<td>{html.escape(result.suite)}</td>"
                f"<td><code>{html.escape(str(result.flow_path))}</code></td>"
                f'<td><span class="badge failed">{result.returncode}</span></td>'
                f"<td>{result.seconds:.1f}</td>"
                f'<td class="analysis">{html.escape(short_text(analysis.classification))}</td>'
                f'<td class="analysis">{html.escape(short_text(analysis.reason))}</td>'
                f'<td class="evidence"><code>{html.escape(short_text(analysis.evidence))}</code></td>'
                f'<td><a href="{report_link(report_path, result.log_path)}">log</a></td>'
                "</tr>"
            )
        lines.extend(["</tbody>", "</table>"])
    else:
        lines.append('<p class="empty">No failed flows in the selected run.</p>')
    lines.append("</section>")

    lines.append("<section>")
    lines.append("<h2>All Tests</h2>")
    for platform in platforms:
        for suite in suites:
            planned_flows = flow_files(repo_root, platform, suite)
            lines.append(
                f"<details open><summary>{html.escape(platform.title())} {html.escape(suite)} "
                f"({len(planned_flows)} tests)</summary>"
            )
            lines.append("<table>")
            lines.append("<thead><tr><th>Result</th><th>Flow</th><th>Seconds</th><th>Analysis</th><th>Log</th></tr></thead>")
            lines.append("<tbody>")
            for flow_path in planned_flows:
                relative_flow = flow_path.relative_to(repo_root)
                result = flow_result_by_key.get((platform, suite, str(relative_flow)))
                status_class = result_class(result.returncode if result else None)
                label = result_label(result.returncode if result else None)
                seconds = f"{result.seconds:.1f}" if result else "-"
                analysis_cell = "-"
                if result and result.returncode != 0:
                    analysis_cell = html.escape(
                        short_text(
                            analyze_failure(
                                result.log_path,
                                result.returncode,
                                str(result.flow_path),
                            ).classification
                        )
                    )
                log_cell = (
                    f'<a href="{report_link(report_path, result.log_path)}">log</a>'
                    if result
                    else "-"
                )
                lines.append(
                    "<tr>"
                    f'<td><span class="badge {status_class}">{label}</span></td>'
                    f"<td><code>{html.escape(str(relative_flow))}</code></td>"
                    f"<td>{seconds}</td>"
                    f'<td class="analysis">{analysis_cell}</td>'
                    f"<td>{log_cell}</td>"
                    "</tr>"
                )
            lines.extend(["</tbody>", "</table>", "</details>"])
    lines.append("</section>")

    lines.extend(["</main>", "</body>", "</html>"])
    report_path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    args = parse_args()
    repo_root = find_repo_root()
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = args.output_dir or repo_root / ".tmp/maestro-release-tests" / timestamp
    output_dir.mkdir(parents=True, exist_ok=True)

    platforms = selected_platforms(args.platform)
    suites = selected_suites(args.suite)

    print_line(f"Repo root: {repo_root}")
    print_line(f"Report directory: {output_dir}")

    missing_tools = [tool for tool in ("maestro", "python3") if not command_exists(tool)]
    if "android" in platforms and not command_exists("adb"):
        missing_tools.append("adb")
    if "ios" in platforms:
        for tool in ("xcrun", "xcodebuild"):
            if not command_exists(tool):
                missing_tools.append(tool)
    invalid_env = [value for value in args.maestro_env if "=" not in value or value.startswith("=")]
    if invalid_env:
        print("Invalid --maestro-env (expected KEY=VALUE): " + ", ".join(invalid_env), file=sys.stderr)
        return 2
    if missing_tools:
        print("Missing required tools: " + ", ".join(missing_tools), file=sys.stderr)
        return 2

    leases: AtcLeases | None = None
    if args.no_atc:
        print_line("Device reservation disabled (--no-atc).")
    elif command_exists("atc"):
        leases = AtcLeases(repo_root, output_dir)
    else:
        print_line("Warning: atc is not installed; the devices are not reserved for this run.")
    # Copy after AtcLeases may have set the session variables, so every child sees them.
    env = os.environ.copy()
    requested_targets = {
        "android": args.android_serial or env.get("ANDROID_SERIAL"),
        "ios": args.ios_udid or env.get("SIMULATOR_UDID"),
    }

    setup_results: dict[str, list[CommandResult]] = {}
    flow_results: list[FlowResult] = []

    def run_platform(platform: str) -> PlatformRunResult:
        return run_platform_tests(
            repo_root,
            output_dir,
            platform,
            suites,
            env,
            requested_targets[platform],
            leases,
            args.maestro_env,
        )

    try:
        if len(platforms) > 1:
            print_line("Running platform suites in parallel.")
            with ThreadPoolExecutor(max_workers=len(platforms)) as executor:
                futures = {platform: executor.submit(run_platform, platform) for platform in platforms}
                platform_results = {platform: futures[platform].result() for platform in platforms}
        else:
            platform_results = {platform: run_platform(platform) for platform in platforms}
    finally:
        if leases is not None:
            leases.release()

    for platform in platforms:
        result = platform_results[platform]
        # A failed restore leaves the device without its development feeds, so it fails the run.
        setup_results[platform] = result.setup_results + (
            [result.restore_result] if result.restore_result is not None else []
        )
        flow_results.extend(result.flow_results)

    markdown_report_path = output_dir / "report.md"
    html_report_path = output_dir / "report.html"
    write_report(markdown_report_path, setup_results, flow_results, platforms, suites)
    write_html_report(
        html_report_path,
        repo_root,
        setup_results,
        flow_results,
        platforms,
        suites,
    )
    failed_setup_count = sum(
        1 for results in setup_results.values() for result in results if result.returncode != 0
    )
    failed_flow_count = sum(1 for result in flow_results if result.returncode != 0)

    print_line(f"\nHTML report written: {html_report_path}")
    print_line(f"Markdown report written: {markdown_report_path}")
    print_line(f"Failures: {failed_setup_count} setup/restore, {failed_flow_count} flows")
    return 1 if failed_setup_count or failed_flow_count else 0


if __name__ == "__main__":
    raise SystemExit(main())
