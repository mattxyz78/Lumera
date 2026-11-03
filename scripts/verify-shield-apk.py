#!/usr/bin/env python3
"""Fail closed before uploading a Cinevo Shield APK. Never accesses signing secrets."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

EXPECTED_PACKAGE = "com.cinevo.tv.test"
EXPECTED_CODE = "13"
EXPECTED_VERSION = "0.2.2-cinevo-test"
EXPECTED_CERT = "c9334a9e5eb9d2254dafa2662b3062ff82396672bd1369f91b7486b87b63ed3c"


def certificate_fingerprints(output):
    # SDK versions label signers either "Signer #1" or "V2 Signer:".
    # A certificate may be printed once for each verified signing scheme.
    pattern = r"(?:Signer #\d+|V[1-4](?:\.\d+)? Signer):? certificate SHA-256 digest: ([a-fA-F0-9]{64})"
    return {match.group(1).lower() for line in output.splitlines()
            if (match := re.fullmatch(pattern, line.strip()))}


def main():
    apks = list(Path("app/build/outputs/apk/debug").glob("*.apk"))
    if len(apks) != 1:
        raise SystemExit(f"Expected exactly one APK, found {len(apks)}")
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"])
    tool_dirs = sorted((sdk / "build-tools").iterdir(),
                       key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.name)))
    tools = next(p for p in reversed(tool_dirs) if (p / "aapt").exists() and (p / "apksigner").exists())
    apk = apks[0]
    badging = subprocess.check_output([str(tools / "aapt"), "dump", "badging", str(apk)], text=True)
    identity = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not identity or identity.groups() != (EXPECTED_PACKAGE, EXPECTED_CODE, EXPECTED_VERSION):
        raise SystemExit("Unexpected APK package/version")
    signature = subprocess.check_output(
        [str(tools / "apksigner"), "verify", "--print-certs", str(apk)], text=True)
    certificates = certificate_fingerprints(signature)
    if certificates != {EXPECTED_CERT}:
        # Certificate fingerprints are public APK metadata, never private keys.
        print(json.dumps({"expectedCertificateSha256": EXPECTED_CERT,
                          "actualCertificateSha256": sorted(certificates)}))
        if not certificates:
            print(signature)
        raise SystemExit("Signing identity differs from Cinevo Test 0.2.1")
    destination = Path("app/build/cinevo")
    destination.mkdir(parents=True, exist_ok=True)
    for previous in destination.glob("*.apk"):
        previous.unlink()
    output = destination / "Cinevo-Test-v0.2.2.apk"
    shutil.copy2(apk, output)
    record = dict(package=EXPECTED_PACKAGE, versionCode=int(EXPECTED_CODE), versionName=EXPECTED_VERSION,
                  certificateSha256=EXPECTED_CERT, apkSha256=hashlib.sha256(output.read_bytes()).hexdigest())
    (destination / "verification.json").write_text(json.dumps(record, indent=2) + "\n")
    print(json.dumps(record, indent=2))


if __name__ == "__main__":
    main()
