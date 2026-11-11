"""Reads the desktop E2E suite's JUnit XML results (see e2e/desktop/README.md)."""

from __future__ import annotations

import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

E2E_PACKAGE = "com.prof18.feedflow.desktop.e2e."
IGNORED_PACKAGES = {"harness", "explore"}


@dataclass
class DesktopCase:
    suite: str  # smoke, regression or desktoponly, from the package
    class_name: str  # simple class name, for example SmokeFlowsTest
    name: str  # method name, for example "SM-001 first launch empty"
    flow_id: str  # text before the first space, for example SM-001
    seconds: float
    passed: bool
    skipped: bool
    details: str  # failure message and stack, then system-out and system-err


def parse_junit_dir(results_dir: Path) -> list[DesktopCase]:
    """Parses every TEST-*.xml in the folder; harness classes are ignored."""
    if not results_dir.is_dir():
        return []
    cases: list[DesktopCase] = []
    for path in sorted(results_dir.glob("TEST-*.xml")):
        suite_element = ElementTree.parse(path).getroot()
        system_out = (suite_element.findtext("system-out") or "").strip()
        system_err = (suite_element.findtext("system-err") or "").strip()
        for case in suite_element.iter("testcase"):
            classname = case.get("classname", "")
            if not classname.startswith(E2E_PACKAGE):
                continue
            relative = classname[len(E2E_PACKAGE):]
            suite = relative.split(".", 1)[0]
            if suite in IGNORED_PACKAGES or "." not in relative:
                continue
            name = case.get("name", "")
            problems = [element for element in case if element.tag in ("failure", "error")]
            skipped = case.find("skipped") is not None
            detail_parts = []
            for problem in problems:
                detail_parts.append(problem.get("message", ""))
                detail_parts.append((problem.text or "").strip())
            for label, text in (("system-out", system_out), ("system-err", system_err)):
                if text:
                    detail_parts.append(f"--- {label} ---\n{text}")
            cases.append(
                DesktopCase(
                    suite=suite,
                    class_name=relative.rsplit(".", 1)[-1],
                    name=name,
                    flow_id=name.split(" ", 1)[0],
                    seconds=float(case.get("time", "0") or 0),
                    passed=not problems and not skipped,
                    skipped=skipped,
                    details="\n".join(part for part in detail_parts if part),
                )
            )
    return cases
