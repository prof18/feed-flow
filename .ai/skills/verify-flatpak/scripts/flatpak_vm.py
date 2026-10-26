#!/usr/bin/env python3
"""Drive FeedFlow Flatpak verification in the local UTM arm64 VM.

The packaging repo on the Mac is the source of truth. It is synced to the VM,
where flatpak-builder runs the real (aarch64) build. x86_64-only inputs are
checked statically: parity against arm64 and a download + hash check.
"""

from __future__ import annotations

import argparse
import getpass
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path

FEEDFLOW_ROOT = Path(__file__).resolve().parents[4]
VM_NAME = os.environ.get("FEEDFLOW_FLATPAK_VM", "feedflow-flatpak")
VM_HOST = os.environ.get("FEEDFLOW_FLATPAK_VM_HOST", f"{getpass.getuser()}@{VM_NAME}.local")
REMOTE_BENCH = "flatpak-bench"
APP_ID = "com.prof18.feedflow"
MANIFEST = f"{APP_ID}.yml"
DEFAULT_REPO = Path(os.environ.get("FEEDFLOW_FLATPAK_REPO", FEEDFLOW_ROOT.parent / APP_ID))
RUNS_DIR = FEEDFLOW_ROOT / ".tmp/flatpak-verify"
FEEDFLOW_GIT_PIN = re.compile(
    r"(url: https://github\.com/prof18/feed-flow\s*\n(\s+))(tag|branch|commit): *\S+"
)
X64_URL_MARKERS = ("linux-x64", "linux-x86_64", "x86_64", "amd64")
PARITY_FAMILIES = (
    ("org/jetbrains/compose/desktop", "desktop-jvm-linux"),
    ("org/jetbrains/skiko", "skiko-awt-runtime-linux"),
)
ARCHES = ("x64", "arm64")
SSH_OPTS = ["-o", "BatchMode=yes", "-o", "ConnectTimeout=5", "-o", "ServerAliveInterval=30"]


def run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, text=True, **kwargs)


def ssh(script: str, **kwargs) -> subprocess.CompletedProcess:
    return run(["ssh", *SSH_OPTS, VM_HOST, script], **kwargs)


def ssh_ok() -> bool:
    return ssh("true", capture_output=True).returncode == 0


def run_dir(name: str) -> Path:
    path = RUNS_DIR / f"{datetime.now():%Y%m%d-%H%M%S}-{name}"
    path.mkdir(parents=True, exist_ok=True)
    return path


def sources_files(repo: Path) -> list[Path]:
    return sorted(repo.glob("flatpak-sources*.json"))


def load_entries(repo: Path) -> list[tuple[str, dict]]:
    return [(f.name, e) for f in sources_files(repo) for e in json.loads(f.read_text())]


def cmd_up(_args) -> int:
    status = run(["utmctl", "status", VM_NAME], capture_output=True).stdout.strip()
    if status != "started":
        print(f"VM is {status or 'unknown'}, starting it")
        if run(["utmctl", "start", VM_NAME]).returncode != 0:
            return 1
    for _ in range(36):
        if ssh_ok():
            print(f"VM ready at {VM_HOST}")
            return 0
        time.sleep(5)
    print("VM did not answer on SSH within 3 minutes", file=sys.stderr)
    return 1


def cmd_preflight(args) -> int:
    failures = 0
    files = sources_files(args.repo)
    if not files:
        print(f"no flatpak-sources*.json in {args.repo}", file=sys.stderr)
        return 1
    seen: dict[tuple[str, str], str] = {}
    cross_file_duplicates = 0
    for f in files:
        try:
            entries = json.loads(f.read_text())
        except json.JSONDecodeError as e:
            print(f"INVALID JSON {f.name}: {e}")
            failures += 1
            continue
        print(f"ok {f.name} ({len(entries)} entries)")
        for e in entries:
            name = e.get("dest-filename", "")
            if "/" in name:
                print(f"  SLASH in dest-filename: {f.name} {e.get('dest')} {name}")
                failures += 1
            key = (e.get("dest", ""), name)
            if key in seen:
                if seen[key] == f.name:
                    print(f"  DUPLICATE within {f.name}: {name}")
                else:
                    cross_file_duplicates += 1
            seen.setdefault(key, f.name)
    # Generators overlap (root/convention/app); flatpak-builder tolerates the same file twice.
    print(f"{cross_file_duplicates} entries repeated across files (expected generator overlap)")
    manuals = [f for f in files if f.name.startswith("flatpak-sources-manual")]
    if len(manuals) > 1:
        print(f"more than one manual sources file: {[f.name for f in manuals]}")
        failures += 1
    pin = FEEDFLOW_GIT_PIN.search((args.repo / MANIFEST).read_text())
    print(f"manifest pin: {pin.group(3)}: {pin.group(0).split(':')[-1].strip()}" if pin else "manifest pin: NOT FOUND")
    return 1 if failures else 0


def pinned_manifest(repo: Path, ref: str | None) -> str:
    text = (repo / MANIFEST).read_text()
    if not ref:
        return text
    kind = "commit" if re.fullmatch(r"[0-9a-f]{40}", ref) else "branch"
    if ref.endswith("-all") or re.fullmatch(r"\d+\.\d+\.\d+.*", ref):
        kind = "tag"
    new, count = FEEDFLOW_GIT_PIN.subn(lambda m: f"{m.group(1)}{kind}: {ref}", text)
    if count != 1:
        raise SystemExit("could not find the feed-flow git source pin in the manifest")
    return new


def cmd_sync(args) -> int:
    if not ssh_ok():
        print("VM not reachable; run `up` first", file=sys.stderr)
        return 1
    files = [f.name for f in sources_files(args.repo)]
    # tar over ssh: macOS ships openrsync, which hangs against the VM's GNU rsync.
    with tempfile.TemporaryDirectory() as staging:
        for name in files:
            (Path(staging) / name).write_bytes((args.repo / name).read_bytes())
        (Path(staging) / MANIFEST).write_text(pinned_manifest(args.repo, args.ref))
        tar = subprocess.Popen(["tar", "-C", staging, "--no-xattrs", "-cf", "-", *files, MANIFEST],
                               stdout=subprocess.PIPE, env={**os.environ, "COPYFILE_DISABLE": "1"})
        rc = run(["ssh", *SSH_OPTS, VM_HOST,
                  f"mkdir -p {REMOTE_BENCH} && rm -f {REMOTE_BENCH}/flatpak-sources*.json"
                  f" && tar -C {REMOTE_BENCH} -xf - 2>/dev/null"],
                 stdin=tar.stdout).returncode
        tar.wait()
    print(f"synced {len(files)} sources files + manifest"
          + (f" (pin overridden to {args.ref})" if args.ref else " (pin unchanged)"))
    return rc


def cmd_build(args) -> int:
    out = run_dir("build")
    flags = "--force-clean --user --install-deps-from=flathub --repo=repo --install"
    if args.disable_cache:
        flags += " --disable-cache"
    if args.keep_build_dirs:
        flags += " --keep-build-dirs"
    log = out / "build.log"
    print(f"building in VM, log: {log}")
    with log.open("w") as fh:
        proc = subprocess.Popen(
            ["ssh", *SSH_OPTS, VM_HOST,
             f"cd {REMOTE_BENCH} && flatpak-builder {flags} builddir {MANIFEST} 2>&1"],
            stdout=subprocess.PIPE, text=True)
        assert proc.stdout
        for line in proc.stdout:
            fh.write(line)
            if args.verbose or re.search(r"Building module|Cache hit|BUILD |FAILURE|error:|Error:", line):
                print(line, end="")
        rc = proc.wait()
    summarize_log(log.read_text())
    print(f"exit code {rc}; log: {log}")
    return rc


def summarize_log(text: str) -> None:
    if "Cache hit for feedflow" in text and "Building module feedflow" not in text:
        print("WARNING: cache hit for feedflow, Gradle did not run; rerun with --disable-cache")
    missing = missing_coordinates(text)
    if missing:
        print(f"{len(missing)} missing dependencies:")
        for c in missing:
            print(f"  {c}")
    if re.search(r"BUILD SUCCESSFUL", text):
        print("Gradle: BUILD SUCCESSFUL")


def missing_coordinates(text: str) -> list[str]:
    found = re.findall(r"Could not (?:resolve|find)(?: all [^:]*for [^\n]*)?\s*:?\s*([\w.\-]+:[\w.\-]+:[\w.\-+]*\w)", text)
    return sorted(set(found))


def cmd_missing(args) -> int:
    log = args.log or latest_build_log()
    if not log:
        print("no build log found", file=sys.stderr)
        return 1
    summarize_log(log.read_text())
    return 0


def latest_build_log() -> Path | None:
    logs = sorted(RUNS_DIR.glob("*-build/build.log"))
    return logs[-1] if logs else None


def family_inventory(entries: list[tuple[str, dict]]) -> dict:
    inv: dict = defaultdict(lambda: defaultdict(set))
    for _, e in entries:
        url = e.get("url", "")
        for group, artifact in PARITY_FAMILIES:
            for arch in ARCHES:
                m = re.search(rf"{group}/{artifact}-{arch}/([^/]+)/[^/]+\.(jar|pom|module)$", url)
                if m:
                    inv[(artifact, arch)][m.group(1)].add(m.group(2))
    return inv


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as resp:
        return resp.read()


def cmd_parity(args) -> int:
    entries = load_entries(args.repo)
    inv = family_inventory(entries)
    problems = 0
    for _, artifact in PARITY_FAMILIES:
        x64, arm = inv[(artifact, "x64")], inv[(artifact, "arm64")]
        print(f"## {artifact}")
        for arch, data in (("x64", x64), ("arm64", arm)):
            print(f"  {arch}: " + ", ".join(f"{v}[{'+'.join(sorted(t))}]" for v, t in sorted(data.items())))
        for version in sorted(set(x64) | set(arm)):
            if x64.get(version) != arm.get(version):
                print(f"  MISMATCH {version}: x64={sorted(x64.get(version, []))} arm64={sorted(arm.get(version, []))}")
                problems += 1
    urls = {e["url"] for _, e in entries if "url" in e}
    skiko = {arch: inv[("skiko-awt-runtime-linux", arch)] for arch in ARCHES}
    for url in sorted(urls):
        m = re.search(r"desktop-jvm-linux-(x64|arm64)/([^/]+)/[^/]+\.pom$", url)
        if not m:
            continue
        arch, version = m.groups()
        pom = fetch(url).decode()
        dep = re.search(rf"<artifactId>skiko-awt-runtime-linux-{arch}</artifactId>\s*<version>([^<]+)</version>", pom)
        if not dep:
            continue
        sv = dep.group(1)
        ok = "jar" in skiko[arch].get(sv, set())
        print(f"  desktop-jvm-linux-{arch} {version} -> skiko {sv}: {'ok' if ok else 'MISSING jar'}")
        problems += 0 if ok else 1
    print("parity OK" if not problems else f"{problems} parity problem(s)")
    return 1 if problems else 0


def is_x86_only(entry: dict) -> bool:
    arches = entry.get("only-arches")
    if arches:
        return "x86_64" in arches and "aarch64" not in arches
    return any(marker in entry.get("url", "") for marker in X64_URL_MARKERS)


def check_entry(entry: dict) -> tuple[str, str]:
    url = entry["url"]
    algo = "sha512" if "sha512" in entry else "sha256"
    try:
        data = fetch(url)
    except urllib.error.HTTPError as e:
        return url, f"HTTP {e.code}"
    except Exception as e:  # network errors are reported per entry
        return url, f"ERROR {e}"
    digest = hashlib.new(algo, data).hexdigest()
    return url, "ok" if digest == entry.get(algo) else f"{algo} MISMATCH"


def cmd_x86(args) -> int:
    manifest_sources = manifest_file_sources(args.repo / MANIFEST)
    candidates = [e for _, e in load_entries(args.repo) if is_x86_only(e)]
    candidates += [e for e in manifest_sources if is_x86_only(e)]
    print(f"checking {len(candidates)} x86_64-only sources (download + hash)")
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(check_entry, candidates))
    bad = [(u, r) for u, r in results if r != "ok"]
    for url, result in bad:
        print(f"  {result}: {url}")
    print(f"{len(results) - len(bad)} ok, {len(bad)} failed")
    return 1 if bad else 0


def manifest_file_sources(manifest: Path) -> list[dict]:
    """Pick inline `type: file` sources (url + hash + only-arches) out of the manifest YAML."""
    entries, current = [], None
    for line in manifest.read_text().splitlines():
        stripped = line.strip()
        if stripped.startswith("- type:"):
            current = {"type": stripped.split(":", 1)[1].strip()}
            entries.append(current)
        elif current is not None and re.match(r"(url|sha256|sha512):", stripped):
            key, value = stripped.split(":", 1)
            current[key] = value.strip().strip('"')
        elif current is not None and stripped.startswith("- ") and "arches" in current:
            current["only-arches"].append(stripped[2:].strip())
        elif current is not None and stripped == "only-arches:":
            current["only-arches"] = []
            current["arches"] = True
    return [e for e in entries if e.get("type") == "file" and "url" in e]


def cmd_smoke(args) -> int:
    out = run_dir("smoke")
    script = f"""
set -u
rm -f /tmp/ff-smoke.png /tmp/ff-smoke.log
xvfb-run -a -s '-screen 0 1440x900x24' bash -c '
  flatpak run --user {APP_ID} > /tmp/ff-smoke.log 2>&1 &
  sleep {args.seconds}
  import -window root /tmp/ff-smoke.png
  flatpak ps --columns=application | grep -q {APP_ID} && echo RUNNING || echo NOT-RUNNING
  flatpak kill {APP_ID} 2>/dev/null
  wait
'
"""
    result = ssh(script, capture_output=True)
    print(result.stdout.strip())
    run(["scp", "-q", f"{VM_HOST}:/tmp/ff-smoke.log", f"{VM_HOST}:/tmp/ff-smoke.png", str(out)])
    log = (out / "ff-smoke.log").read_text() if (out / "ff-smoke.log").exists() else ""
    # The VM has no GPU: Skiko fails OpenGL and falls back to software rendering.
    noise = ("restricted method", "System::load", "LibraryLoader", "GlobalSnapshotManager",
             "WARNING: Use --enable-native-access", "WARNING: A restricted method",
             "Cannot create Linux GL context")
    suspicious = [l for l in log.splitlines()
                  if re.search(r"Exception|Error|FATAL", l) and not any(n in l for n in noise)]
    for line in suspicious[:40]:
        print(f"  {line}")
    print(f"app log + screenshot: {out}")
    return 0 if "RUNNING" in result.stdout.splitlines() and not suspicious else 1


GENERATED_FILES = ("flatpak-sources.json", "flatpak-sources-root.json", "flatpak-sources-convention.json")
INIT_SCRIPT = Path(__file__).with_name("module-sources.init.gradle")
REMOTE_GEN = "flatpak-gen"
# Runs in the VM: union of every module's build/flatpak-sources.json, first entry per destination wins.
MERGE_MODULE_SOURCES = (
    "import glob, json, sys\n"
    "seen = {}\n"
    "for f in sorted(glob.glob(\"**/build/flatpak-sources.json\", recursive=True)):\n"
    "    for e in json.load(open(f)):\n"
    "        seen.setdefault((e[\"dest\"], e[\"dest-filename\"]), e)\n"
    "print(json.dumps(sorted(seen.values(), key=lambda e: (e[\"dest\"], e[\"dest-filename\"])), indent=4))\n"
)


def complete_jars(entries: list[dict]) -> list[dict]:
    """Add `<artifact>-<version>.jar` for components listed without one.

    flatpak-gradle-generator 1.8.0 processes a task's configurations concurrently and records each
    component once; when a configuration that needs only POM/module metadata reaches a component
    first, the jar is silently dropped (seen with compose desktop-jvm and navigation3-ui-desktop).
    BOMs and parent POMs have no jar, so a 404 just means there is nothing to add.
    """
    files_by_dest: dict[str, set[str]] = defaultdict(set)
    sample_url: dict[str, str] = {}
    for e in entries:
        files_by_dest[e["dest"]].add(e["dest-filename"])
        if e["dest-filename"].endswith((".pom", ".module")):
            sample_url.setdefault(e["dest"], e["url"])
    candidates = []
    for dest, url in sample_url.items():
        artifact, version = dest.rstrip("/").split("/")[-2:]
        jar = f"{artifact}-{version}.jar"
        if jar not in files_by_dest[dest]:
            candidates.append((dest, jar, url.rsplit("/", 1)[0] + "/" + jar))

    def probe(candidate):
        dest, jar, url = candidate
        try:
            data = fetch(url)
        except urllib.error.HTTPError:
            return None
        return {"type": "file", "url": url, "sha512": hashlib.sha512(data).hexdigest(),
                "dest": dest, "dest-filename": jar}

    with ThreadPoolExecutor(max_workers=16) as pool:
        added = [e for e in pool.map(probe, candidates) if e]
    entries.extend(added)
    entries.sort(key=lambda e: (e["dest"], e["dest-filename"]))
    return added


def add_x64_natives(entries: list[dict]) -> list[dict]:
    """Mirror every `*-linux-arm64` entry as `*-linux-x64`.

    Generation runs on the arm64 VM, so the Compose plugin only picks the arm64 Linux natives;
    Flathub's x86_64 builder needs the x64 ones with the same file types.
    """
    have = {(e["dest"], e["dest-filename"]) for e in entries}
    candidates = []
    for e in entries:
        if "-linux-arm64/" not in e["url"]:
            continue
        mirrored = {k: (v.replace("linux-arm64", "linux-x64") if isinstance(v, str) else v)
                    for k, v in e.items()}
        if (mirrored["dest"], mirrored["dest-filename"]) not in have:
            candidates.append(mirrored)

    def hashed(entry):
        try:
            data = fetch(entry["url"])
        except urllib.error.HTTPError:
            return None
        entry.pop("sha256", None)
        entry["sha512"] = hashlib.sha512(data).hexdigest()
        return entry

    with ThreadPoolExecutor(max_workers=8) as pool:
        added = [e for e in pool.map(hashed, candidates) if e]
    entries.extend(added)
    entries.sort(key=lambda e: (e["dest"], e["dest-filename"]))
    return added


def cmd_generate(args) -> int:
    """Regenerate the three generated sources files from feed-flow at `ref`, inside the Flatpak SDK."""
    if not ssh_ok():
        print("VM not reachable; run `up` first", file=sys.stderr)
        return 1
    out = run_dir("generate")
    run(["scp", "-q", str(INIT_SCRIPT), f"{VM_HOST}:module-sources.init.gradle"], check=True)
    sdk = "/usr/lib/sdk/openjdk21/jvm/openjdk-21"
    # Same steps as the manifest's feedflow module (setup + disable Android), then the generators.
    inner = f"""set -e
./.scripts/flatpak-build-setup.sh >/dev/null
./.scripts/disable-android-for-flatpak.sh >/dev/null
export JAVA_HOME={sdk} PATH={sdk}/bin:$PATH
GRADLE_ARGS="--console=plain --no-daemon --no-configuration-cache --rerun-tasks"
./gradlew $GRADLE_ARGS --init-script ~/module-sources.init.gradle flatpakGradleGenerator
./gradlew $GRADLE_ARGS -p build-logic convention:flatpakGradleGenerator"""
    script = f"""set -e
rm -rf {REMOTE_GEN} && mkdir {REMOTE_GEN} && cd {REMOTE_GEN}
git init -q && git remote add origin https://github.com/prof18/feed-flow
git fetch -q --depth 1 origin {args.ref} && git checkout -q FETCH_HEAD
git log --oneline -1
url=$(sed -n 's/^distributionUrl=//p' gradle/wrapper/gradle-wrapper.properties | sed 's#\\\\:#:#')
echo "gradle: $url"
curl -sfL -o gradle/wrapper/gradle-bin.zip "$url"
flatpak run --user --share=network --filesystem=home --command=bash org.freedesktop.Sdk//25.08 -c '{inner}'
"""
    log = out / "generate.log"
    print(f"generating from feed-flow {args.ref} in the VM, log: {log}")
    with log.open("w") as fh:
        rc = ssh(script, stdout=fh, stderr=subprocess.STDOUT).returncode
    text = log.read_text()
    print("\n".join(l for l in text.splitlines()[:2]))
    if rc != 0 or "BUILD SUCCESSFUL" not in text:
        print(f"generation failed (exit {rc}); see {log}")
        return 1
    gradle_url = re.search(r"^gradle: (\S+)", text, re.M)
    manifest = (args.repo / MANIFEST).read_text()
    if gradle_url and Path(gradle_url.group(1)).name not in manifest:
        print(f"WARNING: feed-flow uses {Path(gradle_url.group(1)).name}; bump the Gradle source "
              "(url + sha256) in the manifest")
    merged = ssh(f"cd {REMOTE_GEN} && python3 -c '{MERGE_MODULE_SOURCES}' "
                 f"> desktopApp/packaging/flatpak/flatpak-sources.json", capture_output=True)
    if merged.returncode != 0:
        print(f"merging module sources failed: {merged.stderr}")
        return 1
    for name in GENERATED_FILES:
        run(["scp", "-q", f"{VM_HOST}:{REMOTE_GEN}/desktopApp/packaging/flatpak/{name}", str(out)], check=True)
        entries = json.loads((out / name).read_text())
        kept = [e for e in entries if "/" not in e.get("dest-filename", "")]
        dropped = len(entries) - len(kept)
        if name == "flatpak-sources.json":
            added = complete_jars(kept)
            print(f"{name}: added {len(added)} jars the plugin dropped")
            x64 = add_x64_natives(kept)
            print(f"{name}: added {len(x64)} linux-x64 natives mirroring linux-arm64")
        old = len(json.loads((args.repo / name).read_text())) if (args.repo / name).exists() else 0
        (args.repo / name).write_text(json.dumps(kept, indent=4) + "\n")
        print(f"{name}: {old} -> {len(kept)} entries"
              + (f" ({dropped} slash dest-filename entry dropped)" if dropped else ""))
    print(f"written to {args.repo}; review with git diff there, then sync + build")
    return 0


def cmd_gui(_args) -> int:
    # The GNOME user manager already holds DISPLAY/XAUTHORITY for Xwayland.
    script = f"""
export XDG_RUNTIME_DIR=/run/user/$(id -u)
systemctl --user stop feedflow-gui 2>/dev/null
systemd-run --user --collect --unit=feedflow-gui flatpak run --user {APP_ID} >/dev/null
sleep 8
systemctl --user is-active feedflow-gui
"""
    result = ssh(script, capture_output=True)
    state = result.stdout.strip()
    print(f"FeedFlow in the GNOME session: {state} (logs: journalctl --user -u feedflow-gui)")
    return 0 if state == "active" else 1


def main() -> int:
    sys.stdout.reconfigure(line_buffering=True)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=DEFAULT_REPO, help="packaging repo checkout")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("up", help="start the VM and wait for SSH")
    sub.add_parser("preflight", help="validate the sources JSON locally")
    sync = sub.add_parser("sync", help="copy manifest + sources to the VM")
    sync.add_argument("--ref", help="override the feed-flow pin in the VM copy (branch, tag or commit)")
    build = sub.add_parser("build", help="run flatpak-builder in the VM")
    build.add_argument("--disable-cache", action="store_true")
    build.add_argument("--keep-build-dirs", action="store_true")
    build.add_argument("--verbose", action="store_true")
    missing = sub.add_parser("missing", help="list missing coordinates from a build log")
    missing.add_argument("--log", type=Path)
    sub.add_parser("parity", help="static x64/arm64 native artifact parity")
    sub.add_parser("x86", help="download and hash-check x86_64-only sources")
    smoke = sub.add_parser("smoke", help="headless launch + screenshot in the VM")
    smoke.add_argument("--seconds", type=int, default=25)
    sub.add_parser("gui", help="open the installed app in the VM's GNOME desktop")
    generate = sub.add_parser("generate", help="regenerate the generated sources files from feed-flow")
    generate.add_argument("--ref", default="main", help="feed-flow branch, tag or commit")
    args = parser.parse_args()
    args.repo = args.repo.expanduser().resolve()
    handlers = {
        "up": cmd_up, "preflight": cmd_preflight, "sync": cmd_sync, "build": cmd_build,
        "missing": cmd_missing, "parity": cmd_parity, "x86": cmd_x86, "smoke": cmd_smoke, "gui": cmd_gui, "generate": cmd_generate,
    }
    return handlers[args.command](args)


if __name__ == "__main__":
    sys.exit(main())
