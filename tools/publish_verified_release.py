#!/usr/bin/env python3
"""Publish only packages from successful workflows for the exact checked-out commit.

Run after local checks and a clean commit/push. This intentionally refuses to
publish while the documented optional-Full-Access decision is unresolved.
"""
import hashlib
import json
import os
from pathlib import Path
import plistlib
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPO = "Krutas/Android-keyboard-for-developers"
VERSION = "0.2.6"


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def main():
    if run("git", "status", "--porcelain"):
        raise SystemExit("Refusing to publish a dirty checkout")
    audit = (ROOT / "docs/PARITY-0.2.6.md").read_text()
    if "awaits user approval" in audit or "pending explicit approval" in audit:
        raise SystemExit("Release blocked: optional iOS Full Access decision remains unresolved")
    sha = run("git", "rev-parse", "HEAD")
    runs = json.loads(run("gh", "api", f"repos/{REPO}/actions/runs?head_sha={sha}&per_page=100"))["workflow_runs"]
    selected = {}
    for workflow in ["android.yml", "ios.yml", "web.yml", "pages.yml"]:
        matches = sorted((r for r in runs if r["path"] == f".github/workflows/{workflow}"), key=lambda r: r["id"], reverse=True)
        if not matches or matches[0]["status"] != "completed" or matches[0]["conclusion"] != "success":
            raise SystemExit(f"Release blocked: {workflow} has not succeeded for {sha}")
        selected[workflow] = matches[0]
    with tempfile.TemporaryDirectory(prefix="devboard-publish-") as folder:
        target = Path(folder)
        for workflow in ["android.yml", "ios.yml"]:
            run("gh", "run", "download", str(selected[workflow]["id"]), "--repo", REPO, "--dir", str(target / workflow))
        apks = list((target / "android.yml").rglob("*.apk"))
        if len(apks) != 1:
            raise SystemExit("Expected exactly one Android APK")
        apk = target / f"DevBoard-v{VERSION}.apk"
        apk.write_bytes(apks[0].read_bytes())
        # Compare metadata with the locally verified build; CI debug signing keys differ.
        local = Path(os.environ.get("DEVBOARD_VERIFIED_APK", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk")))
        if not local.exists():
            raise SystemExit("Missing locally verified Android release APK")
        # Exact source workflow checks establish provenance; the manifest checks version/package.
        with zipfile.ZipFile(apk) as remote, zipfile.ZipFile(local) as built:
            for name in ["AndroidManifest.xml"]:
                if remote.read(name) != built.read(name):
                    raise SystemExit(f"Android CI/local payload mismatch: {name}")
        packages = [apk]
        for suffix in ["Simulator.zip", "unsigned.ipa"]:
            archives = list((target / "ios.yml").rglob(f"DevBoard-iOS-v{VERSION}-{suffix}"))
            if len(archives) != 1:
                raise SystemExit(f"Expected exactly one iOS {suffix}")
            archive = archives[0]
            with zipfile.ZipFile(archive) as z:
                paths = [n for n in z.namelist() if n.endswith("DevBoard.app/Info.plist") or n.endswith("DevBoardKeyboard.appex/Info.plist")]
                if len(paths) != 2:
                    raise SystemExit(f"Missing app/extension metadata: {archive.name}")
                for name in paths:
                    meta = plistlib.loads(z.read(name))
                    if meta.get("CFBundleShortVersionString") != VERSION or meta.get("CFBundleVersion") != "4":
                        raise SystemExit(f"Wrong version: {archive.name}: {name}")
            packages.append(archive)
        notes = target / "notes.md"
        notes.write_text(f"DevBoard {VERSION} developer preview. Android is debug-signed; iPhone IPA is unsigned and requires Apple signing. No physical-device verification is claimed.\n\n"
                         f"Source: {sha}\n\n" + "\n".join(f"- [{name}]({r['html_url']})" for name, r in selected.items()) +
                         f"\n\n[Feature audit and remaining limitations](https://github.com/{REPO}/blob/{sha}/docs/PARITY-0.2.6.md).\n")
        checksum = target / "SHA256SUMS.txt"
        checksum.write_text("".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n" for p in packages))
        run("gh", "release", "create", f"v{VERSION}", "--repo", REPO, "--target", sha,
            "--title", f"DevBoard {VERSION} developer preview", "--prerelease", "--notes-file", str(notes),
            *map(str, packages), str(checksum))
        print(f"Published verified artifacts for {sha}")


if __name__ == "__main__":
    main()
