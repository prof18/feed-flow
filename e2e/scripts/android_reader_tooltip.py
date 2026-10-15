"""Run the original reader assertions while holding a native Android long press."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
from typing import Any
import xml.etree.ElementTree as ET


_APP_ID = "com.prof18.feedflow.debug"
_TOOLTIP_ID = "reader_toolbar_tooltip"
_BUTTON_ID = "reader_browser_button"


def split_flow(source: str) -> tuple[str, str, str, str]:
    """Split the checked-in flow around its validated tooltip assertion."""
    lines = source.splitlines(keepends=True)
    separators = [index for index, line in enumerate(lines) if line.rstrip("\r\n") == "---"]
    if len(separators) != 1:
        raise ValueError("expected one YAML document separator")

    separator = separators[0]
    header = "".join(lines[: separator + 1])
    if header.rstrip("\r\n") != f"appId: {_APP_ID}\n---":
        raise ValueError("unexpected Maestro flow header or app id")

    body_lines = lines[separator + 1 :]
    starts = [index for index, line in enumerate(body_lines) if line.startswith("- ")]
    if not starts or any(line.strip() for line in body_lines[: starts[0]]):
        raise ValueError("expected top-level Maestro steps after header")

    steps = []
    for index, start in enumerate(starts):
        end = starts[index + 1] if index + 1 < len(starts) else len(body_lines)
        steps.append("".join(body_lines[start:end]))

    swipes = [index for index, step in enumerate(steps) if re.search(r"(?m)^- swipe:\s*$", step)]
    if len(swipes) != 1:
        raise ValueError("expected exactly one stationary tooltip swipe")
    swipe_index = swipes[0]
    swipe = steps[swipe_index]
    start_match = re.search(r"(?m)^\s+start:\s*(.+?)\s*$", swipe)
    end_match = re.search(r"(?m)^\s+end:\s*(.+?)\s*$", swipe)
    duration_match = re.search(r"(?m)^\s+duration:\s*(\d+)\s*$", swipe)
    if not start_match or not end_match or not duration_match:
        raise ValueError("stationary tooltip swipe is missing start, end, or duration")
    if start_match.group(1) != end_match.group(1) or duration_match.group(1) != "800":
        raise ValueError("tooltip gesture must remain the original stationary 800 ms swipe")
    if start_match.group(1) != "21%, 89%":
        raise ValueError("tooltip gesture start changed from the checked-in flow")

    presence_indices = [
        index for index, step in enumerate(steps)
        if re.search(rf"(?m)^- assertVisible:\s*\n\s+id:\s*{_TOOLTIP_ID}\s*$", step)
    ]
    disappearance_indices = [
        index for index, step in enumerate(steps)
        if re.search(rf"(?m)^- extendedWaitUntil:\s*\n\s+notVisible:\s*\n\s+id:\s*{_TOOLTIP_ID}\s*$", step)
    ]
    if len(presence_indices) != 1 or presence_indices[0] != swipe_index + 1:
        raise ValueError("expected one tooltip presence assertion immediately after the gesture")
    presence_index = presence_indices[0]
    if len(disappearance_indices) != 1 or disappearance_indices[0] != presence_index + 1:
        raise ValueError("expected one tooltip disappearance wait immediately after presence")

    setup_steps = steps[:swipe_index]
    setup = "".join(setup_steps)
    required_setup = (
        "- launchApp",
        "feedflow://e2e/reset-and-seed?profile=reader-mode",
        '- assertVisible: "E2E seed complete"',
        "- tapOn:\n    id: e2e_open_app",
        "- scrollUntilVisible:",
        "id: article_row_e2e_article_reader_success",
        "- tapOn:\n    id: article_row_e2e_article_reader_success",
        "- assertVisible:\n    id: reader_article_e2e_article_reader_success",
        "- assertVisible:\n    id: reader_browser_button",
    )
    positions = [setup.find(fragment) for fragment in required_setup]
    if any(position < 0 for position in positions) or positions != sorted(positions):
        raise ValueError("reader-mode setup lost or reordered a required seed/assertion step")

    presence = steps[presence_index]
    tail = "".join(steps[presence_index + 1 :])
    if not tail.startswith("- extendedWaitUntil:"):
        raise ValueError("tooltip disappearance wait must begin the tail")
    required_tail = (
        "id: reader_toolbar_tooltip",
        "- tapOn:\n    id: reader_next_button",
        "id: reader_article_e2e_article_reader_fallback",
        "- tapOn:\n    id: reader_bookmark_button",
        "- tapOn:\n    id: reader_more_menu_button",
        'text: "Text Settings"',
        'visible: "Font Size"',
        '- assertVisible: "Line Height"',
    )
    tail_positions = [tail.find(fragment) for fragment in required_tail]
    if any(position < 0 for position in tail_positions) or tail_positions != sorted(tail_positions):
        raise ValueError("reader-action tail lost or reordered an original assertion/action")
    return header, setup, presence, tail


def button_center(layout: Any) -> tuple[int, int]:
    """Return the observed center of the sole enabled, long-clickable browser button."""
    matches = []

    def visit(value: Any) -> None:
        if isinstance(value, dict):
            resource_id = value.get("resource-id", value.get("resourceId", value.get("resource_id")))
            if resource_id == _BUTTON_ID:
                matches.append(value)
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(layout)
    if len(matches) != 1:
        raise ValueError(f"expected one {_BUTTON_ID}, found {len(matches)}")
    target = matches[0]

    state = target.get("state")
    enabled = target.get("enabled")
    disabled_flag = False
    if isinstance(state, dict):
        enabled = state.get("enabled", enabled)
        flags = state.get("flags", [])
        if isinstance(flags, str):
            flags = re.split(r"[,\s]+", flags)
        disabled_flag = "DISABLED" in {str(flag).upper() for flag in flags}
    elif isinstance(state, str) and "enabled" in state.lower():
        enabled = "disabled" not in state.lower()
    elif isinstance(state, list):
        disabled_flag = "DISABLED" in {str(flag).upper() for flag in state}
    if enabled is not True or disabled_flag:
        raise ValueError(f"{_BUTTON_ID} is not positively confirmed enabled")

    interactions = target.get("interactions", [])
    if isinstance(interactions, dict):
        interaction_names = {str(name).upper() for name, active in interactions.items() if active}
    elif isinstance(interactions, list):
        interaction_names = {str(item).upper() for item in interactions}
    elif isinstance(interactions, str):
        interaction_names = {item.upper() for item in re.split(r"[,\s]+", interactions) if item}
    else:
        interaction_names = set()
    if "LONG_CLICKABLE" not in interaction_names:
        raise ValueError(f"{_BUTTON_ID} is not long-clickable")

    bounds = target.get("bounds")
    bounds_match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds or "")
    if not bounds_match:
        raise ValueError(f"{_BUTTON_ID} has invalid screen bounds")
    left, top, right, bottom = map(int, bounds_match.groups())
    if right <= left or bottom <= top:
        raise ValueError(f"{_BUTTON_ID} bounds have no area")

    center = target.get("center")
    if isinstance(center, dict):
        center = [center.get("x"), center.get("y")]
    elif isinstance(center, str):
        center_match = re.fullmatch(r"\[\s*(-?\d+)\s*,\s*(-?\d+)\s*\]", center)
        center = list(map(int, center_match.groups())) if center_match else None
    if not isinstance(center, (list, tuple)) or len(center) != 2:
        raise ValueError(f"{_BUTTON_ID} has no observed center")
    x, y = center
    if any(not isinstance(value, int) or isinstance(value, bool) for value in (x, y)):
        raise ValueError(f"{_BUTTON_ID} center is not a pair of integer coordinates")
    if not (left <= x < right and top <= y < bottom):
        raise ValueError(f"{_BUTTON_ID} center lies outside its screen bounds")
    return x, y


def native_button_layout(native_xml: str) -> dict[str, Any]:
    """Validate the seeded native reader and return its unique browser-button geometry."""
    root = ET.fromstring(native_xml)
    reader_nodes = [node for node in root.iter("node")
                    if node.get("resource-id") == "reader_article_e2e_article_reader_success"
                    and node.get("package") == _APP_ID]
    if not reader_nodes:
        raise ValueError("native hierarchy did not contain the seeded reader in the app")

    targets = [node for node in root.iter("node") if node.get("resource-id") == _BUTTON_ID]
    if len(targets) != 1:
        raise ValueError(f"expected one native {_BUTTON_ID}, found {len(targets)}")
    target = targets[0]
    if target.get("package") != _APP_ID:
        raise ValueError(f"native {_BUTTON_ID} belongs to an unexpected package")
    for attribute in ("enabled", "clickable", "long-clickable"):
        if target.get(attribute) != "true":
            raise ValueError(f"native {_BUTTON_ID} is not confirmed {attribute}")

    bounds_match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]",
                                target.get("bounds", ""))
    if not bounds_match:
        raise ValueError(f"native {_BUTTON_ID} has invalid screen bounds")
    left, top, right, bottom = map(int, bounds_match.groups())
    if left < 0 or top < 0 or right <= left or bottom <= top:
        raise ValueError(f"native {_BUTTON_ID} bounds have no area")
    center = [(left + right) // 2, (top + bottom) // 2]
    return {"windows": [{"content": [{
        "resource-id": _BUTTON_ID,
        "bounds": target.get("bounds"),
        "center": center,
        "enabled": True,
        "interactions": ["LONG_CLICKABLE"],
    }]}]}


def matching_cli_buttons(layout: Any) -> list[dict[str, Any]]:
    """Find every matching Android CLI node without silently choosing among duplicates."""
    matches = []

    def visit(value: Any) -> None:
        if isinstance(value, dict):
            resource_id = value.get("resource-id", value.get("resourceId", value.get("resource_id")))
            if resource_id == _BUTTON_ID:
                matches.append(value)
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(layout)
    return matches


class CommandRunner:
    def run(self, command, log, timeout):
        try:
            result = subprocess.run(command, stdin=subprocess.DEVNULL, capture_output=True,
                                    text=True, timeout=timeout, check=False)
        except subprocess.TimeoutExpired as error:
            log.write_text(f"Timed out after {timeout}s\n{error.stdout or ''}\n{error.stderr or ''}")
            raise
        log.write_text(result.stdout + result.stderr)
        return result


def run_journey(device, flow, output, *, runner=None, sleep=time.sleep):
    source = flow.read_text()
    header, setup, presence, tail = split_flow(source)
    output.mkdir(parents=True, exist_ok=False)
    runner = runner or CommandRunner()
    deadline = time.monotonic() + 180
    result = {"device": device, "sourceSha256": hashlib.sha256(source.encode()).hexdigest(),
              "passed": False, "pressActive": False, "phases": {}, "coordinateSource": None}
    point = None

    def command(name, args, *, cleanup=False):
        timeout = 10 if cleanup else min(60, deadline - time.monotonic())
        if timeout <= 0:
            raise TimeoutError("reader journey exceeded its three-minute limit")
        completed = runner.run(args, output / f"{name}.log", timeout)
        result["phases"][name] = completed.returncode
        if completed.returncode:
            raise RuntimeError(f"{name} failed with exit {completed.returncode}")
        return completed

    def phase(name, body):
        path = output / f"{name}.yaml"
        path.write_text(header + body)
        args = ["maestro", f"--device={device}", "--platform", "android", "test"]
        if name != "setup":
            args.append("--no-reinstall-driver")
        command(name, args + ["--format", "JUNIT", "--output", str(output / f"{name}.xml"),
                             "--test-output-dir", str(output / f"{name}-artifacts"), str(path)])

    def touch(action, *, cleanup=False):
        command(action.lower(), ["adb", "-s", device, "shell", "input", "touchscreen",
                                 "motionevent", action, *map(str, point)], cleanup=cleanup)

    try:
        command("release-initial-layout-driver", ["adb", "-s", device, "shell", "am", "force-stop",
                                                  "com.android.cli.interact.instrumentation"])
        phase("setup", setup)
        layout_path = output / "reader-layout.json"
        command("layout", ["android", "layout", f"--device={device}", "--full", "--no-idle",
                           "--flat", f"--output={layout_path}"])
        layout = json.loads(layout_path.read_text())
        # Only one UiAutomation connection can own the device at a time.
        command("release-layout-driver", ["adb", "-s", device, "shell", "am", "force-stop",
                                          "com.android.cli.interact.instrumentation"])
        remote = f"/data/local/tmp/feedflow-tooltip-{time.time_ns()}.xml"
        command("native-dump", ["adb", "-s", device, "shell", "uiautomator", "dump", remote])
        native = command("native-layout", ["adb", "-s", device, "exec-out", "cat", remote])
        native_layout = native_button_layout(native.stdout)
        cli_targets = matching_cli_buttons(layout)
        if len(cli_targets) > 1:
            raise ValueError(f"expected at most one Android CLI {_BUTTON_ID}, found {len(cli_targets)}")
        if cli_targets:
            cli_target = cli_targets[0]
            native_target = native_layout["windows"][0]["content"][0]
            if cli_target.get("bounds") != native_target["bounds"]:
                raise ValueError("native and Android CLI browser bounds disagree")
            # Native UIAutomator confirmed enabled=true; do not override an explicit CLI disabled state.
            state = cli_target.get("state")
            if cli_target.get("enabled") is False or str(cli_target.get("enabled", "")).lower() == "false":
                raise ValueError(f"Android CLI {_BUTTON_ID} is explicitly disabled")
            if isinstance(state, dict):
                flags = state.get("flags", [])
                if isinstance(flags, str):
                    flags = re.split(r"[,\s]+", flags)
                state_disabled = state.get("enabled") is False or str(state.get("enabled", "")).lower() == "false"
                if state_disabled or "DISABLED" in {str(flag).upper() for flag in flags}:
                    raise ValueError(f"Android CLI {_BUTTON_ID} is explicitly disabled")
            cli_target["enabled"] = True
            point = button_center(layout)
            result["coordinateSource"] = "android-cli"
        else:
            point = button_center(native_layout)
            result["coordinateSource"] = "native-uiautomator"
        result["point"] = point
        result["pressActive"] = True
        try:
            touch("DOWN")
            sleep(0.8)
            phase("presence", presence)
        finally:
            touch("UP", cleanup=True)
            result["pressActive"] = False
        phase("tail", tail)
        result["passed"] = True
    except (OSError, ValueError, RuntimeError, TimeoutError, subprocess.TimeoutExpired) as error:
        result["error"] = str(error)
    finally:
        if result["pressActive"] and point is not None:
            try:
                touch("UP", cleanup=True)
                result["pressActive"] = False
            except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
                result["cleanupError"] = str(error)
                result["passed"] = False
        (output / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True)
    parser.add_argument("--flow", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path(".tmp") / f"android-reader-tooltip-{time.time_ns()}")
    parser.add_argument("--parent-restores", action="store_true", required=True,
                        help="The calling runner must restore development feeds in its EXIT trap")
    args = parser.parse_args()
    result = run_journey(args.device, args.flow, args.output.resolve())
    print(json.dumps(result, indent=2))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
