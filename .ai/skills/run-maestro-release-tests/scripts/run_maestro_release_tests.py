#!/usr/bin/env python3
"""Run FeedFlow Maestro release tests and write Markdown and HTML reports."""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
import html
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
IOS_SIMULATOR_NAME = "iPhone 17 Pro"
SUITES = ("smoke", "regression")
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
        for line in process.stdout:
            print_text(line)
            log_file.write(line)
        returncode = process.wait()
    seconds = time.monotonic() - start
    print_line(f"<== {name}: exit {returncode} in {seconds:.1f}s")
    return CommandResult(name=name, returncode=returncode, seconds=seconds, log_path=log_path)


def print_line(message: str) -> None:
    with PRINT_LOCK:
        print(message)


def print_text(message: str) -> None:
    with PRINT_LOCK:
        print(message, end="")


def selected_platforms(platform_arg: str) -> tuple[str, ...]:
    if platform_arg == "all":
        return ("android", "ios")
    return (platform_arg,)


def selected_suites(suite_arg: str) -> tuple[str, ...]:
    if suite_arg == "all":
        return SUITES
    return (suite_arg,)


def setup_android(repo_root: Path, log_root: Path) -> list[CommandResult]:
    results: list[CommandResult] = []
    apk_path = repo_root / "androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"

    results.append(
        run_command(
            "android build",
            ["./gradlew", "--quiet", "--console=plain", ":androidApp:assembleGooglePlayDebug"],
            repo_root,
            log_root / "setup/android/01-build.log",
        )
    )
    if results[-1].returncode != 0:
        return results

    if command_exists("android"):
        install_args = ["android", "run", f"--apks={apk_path}"]
    else:
        install_args = ["adb", "install", "-r", str(apk_path)]
    results.append(
        run_command(
            "android install",
            install_args,
            repo_root,
            log_root / "setup/android/02-install.log",
        )
    )
    if results[-1].returncode != 0:
        return results

    if not command_exists("android"):
        results.append(
            run_command(
                "android launch",
                ["adb", "shell", "monkey", "-p", ANDROID_APP_ID, "1"],
                repo_root,
                log_root / "setup/android/03-launch.log",
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
        )
    )
    return results


def booted_ios_simulator_udid(repo_root: Path, log_root: Path) -> tuple[str | None, CommandResult]:
    result = run_command(
        "ios simulator lookup",
        ["xcrun", "simctl", "list", "devices", "booted"],
        repo_root,
        log_root / "setup/ios/00-simulator-lookup.log",
    )
    if result.returncode != 0:
        return None, result

    text = result.log_path.read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        if IOS_SIMULATOR_NAME in line:
            match = re.search(r"\(([A-Fa-f0-9-]{36})\)", line)
            if match:
                return match.group(1), result
    return None, result


def setup_ios(repo_root: Path, log_root: Path, env: dict[str, str]) -> list[CommandResult]:
    results: list[CommandResult] = []
    simulator_udid = env.get("SIMULATOR_UDID")
    if not simulator_udid:
        simulator_udid, lookup_result = booted_ios_simulator_udid(repo_root, log_root)
        results.append(lookup_result)
    if not simulator_udid:
        missing_log = log_root / "setup/ios/01-missing-simulator.log"
        missing_log.parent.mkdir(parents=True, exist_ok=True)
        missing_log.write_text(
            f"No booted {IOS_SIMULATOR_NAME} simulator found. Boot it before running iOS E2E flows.\n",
            encoding="utf-8",
        )
        results.append(CommandResult("ios simulator preflight", 1, 0.0, missing_log))
        return results

    env = {**env, "SIMULATOR_UDID": simulator_udid}
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
                f"platform=iOS Simulator,name={IOS_SIMULATOR_NAME}",
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


def run_flows(
    repo_root: Path,
    log_root: Path,
    platform: str,
    suites: tuple[str, ...],
    env: dict[str, str],
) -> list[FlowResult]:
    results: list[FlowResult] = []
    for suite in suites:
        flows = flow_files(repo_root, platform, suite)
        print(f"\n==> {platform} {suite}: {len(flows)} flows")
        for flow_path in flows:
            relative_flow = flow_path.relative_to(repo_root)
            log_path = log_root / "flows" / platform / suite / safe_log_name(flow_path)
            if platform == "ios":
                args = [
                    "maestro",
                    "--platform",
                    "ios",
                    "--device",
                    env["SIMULATOR_UDID"],
                    "test",
                    str(relative_flow),
                ]
            else:
                args = ["maestro", "--platform", "android", "test", str(relative_flow)]
            command_result = run_command(
                f"{platform} {suite} {flow_path.name}",
                args,
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


def update_ios_simulator_udid(env: dict[str, str], setup_results: list[CommandResult]) -> None:
    if "SIMULATOR_UDID" in env:
        return
    lookup_logs = [
        result.log_path
        for result in setup_results
        if result.name == "ios simulator lookup"
    ]
    if not lookup_logs:
        return
    text = lookup_logs[-1].read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        if IOS_SIMULATOR_NAME in line:
            match = re.search(r"\(([A-Fa-f0-9-]{36})\)", line)
            if match:
                env["SIMULATOR_UDID"] = match.group(1)
                return


def run_platform_tests(
    repo_root: Path,
    output_dir: Path,
    platform: str,
    suites: tuple[str, ...],
    base_env: dict[str, str],
) -> PlatformRunResult:
    env = base_env.copy()
    if platform == "android":
        setup_results = setup_android(repo_root, output_dir)
    else:
        setup_results = setup_ios(repo_root, output_dir, env)
        update_ios_simulator_udid(env, setup_results)

    if setup_failed(setup_results):
        print_line(f"Skipping {platform} flows because setup failed.")
        return PlatformRunResult(platform, setup_results, [])

    return PlatformRunResult(
        platform,
        setup_results,
        run_flows(repo_root, output_dir, platform, suites, env),
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
    env = os.environ.copy()

    print_line(f"Repo root: {repo_root}")
    print_line(f"Report directory: {output_dir}")

    missing_tools = [tool for tool in ("maestro",) if not command_exists(tool)]
    if "android" in platforms and not command_exists("android") and not command_exists("adb"):
        missing_tools.append("android or adb")
    if "ios" in platforms:
        for tool in ("xcrun", "xcodebuild"):
            if not command_exists(tool):
                missing_tools.append(tool)
    if missing_tools:
        print("Missing required tools: " + ", ".join(missing_tools), file=sys.stderr)
        return 2

    setup_results: dict[str, list[CommandResult]] = {}
    flow_results: list[FlowResult] = []

    if len(platforms) > 1:
        print_line("Running platform suites in parallel.")
        with ThreadPoolExecutor(max_workers=len(platforms)) as executor:
            futures = {
                platform: executor.submit(
                    run_platform_tests,
                    repo_root,
                    output_dir,
                    platform,
                    suites,
                    env,
                )
                for platform in platforms
            }
            platform_results = {platform: futures[platform].result() for platform in platforms}
    else:
        platform_results = {
            platform: run_platform_tests(repo_root, output_dir, platform, suites, env)
            for platform in platforms
        }

    for platform in platforms:
        result = platform_results[platform]
        setup_results[platform] = result.setup_results
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
    print_line(f"Failures: {failed_setup_count} setup, {failed_flow_count} flows")
    return 1 if failed_setup_count or failed_flow_count else 0


if __name__ == "__main__":
    raise SystemExit(main())
