#!/usr/bin/env python3
"""Pull Google Play listing content through the official Publishing API.

The API requires a Play edit for listing and image reads. This script creates one
disposable edit, reads it, deletes it, and never commits or publishes anything.
"""

from __future__ import annotations

import argparse
import base64
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen


API_BASE_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3"
OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
OAUTH_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
IMAGE_TYPES = (
    "icon",
    "featureGraphic",
    "phoneScreenshots",
    "sevenInchScreenshots",
    "tenInchScreenshots",
)
REQUEST_TIMEOUT_SECONDS = 30


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--credentials",
        default=os.environ.get("FEEDFLOW_PLAY_CONFIG_JSON"),
        help="Service-account JSON path (defaults to FEEDFLOW_PLAY_CONFIG_JSON).",
    )
    parser.add_argument("--package-name", default="com.prof18.feedflow", help="Google Play package name.")
    parser.add_argument(
        "--output-dir",
        type=Path,
        required=True,
        help="Empty directory where the JSON audit output will be written.",
    )
    return parser.parse_args()


def base64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def sign_jwt(private_key: str, message: bytes) -> bytes:
    temporary_key_path: str | None = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", delete=False) as key_file:
            key_file.write(private_key)
            temporary_key_path = key_file.name
        os.chmod(temporary_key_path, 0o600)
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", temporary_key_path],
            input=message,
            capture_output=True,
            check=False,
        )
        if result.returncode != 0:
            raise RuntimeError(result.stderr.decode("utf-8", errors="replace").strip())
        return result.stdout
    except FileNotFoundError as error:
        raise RuntimeError("openssl is required to sign the service-account JWT") from error
    finally:
        if temporary_key_path:
            Path(temporary_key_path).unlink(missing_ok=True)


def request_json(
    method: str,
    url: str,
    *,
    token: str | None = None,
    body: bytes | None = None,
    content_type: str | None = None,
) -> dict[str, Any]:
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if content_type:
        headers["Content-Type"] = content_type
    request = Request(url, data=body, headers=headers, method=method)
    try:
        with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            payload = response.read()
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Google Play API {method} {url} failed ({error.code}): {detail}") from error
    except URLError as error:
        raise RuntimeError(f"Google Play API {method} {url} failed: {error.reason}") from error
    return json.loads(payload) if payload else {}


def access_token(credentials: dict[str, Any]) -> str:
    missing_fields = [field for field in ("client_email", "private_key") if not credentials.get(field)]
    if missing_fields:
        raise RuntimeError(f"Service-account JSON is missing: {', '.join(missing_fields)}")
    now = int(time.time())
    header = base64url(json.dumps({"alg": "RS256", "typ": "JWT"}, separators=(",", ":")).encode())
    claims = base64url(
        json.dumps(
            {
                "iss": credentials["client_email"],
                "scope": OAUTH_SCOPE,
                "aud": OAUTH_TOKEN_URL,
                "iat": now,
                "exp": now + 3600,
            },
            separators=(",", ":"),
        ).encode()
    )
    unsigned_token = f"{header}.{claims}"
    signature = base64url(sign_jwt(credentials["private_key"], unsigned_token.encode("ascii")))
    response = request_json(
        "POST",
        OAUTH_TOKEN_URL,
        body=urlencode(
            {
                "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                "assertion": f"{unsigned_token}.{signature}",
            }
        ).encode("utf-8"),
        content_type="application/x-www-form-urlencoded",
    )
    try:
        return response["access_token"]
    except KeyError as error:
        raise RuntimeError("OAuth response did not include an access token") from error


def api_url(package_name: str, path: str) -> str:
    return f"{API_BASE_URL}/applications/{quote(package_name, safe='')}/{path}"


def ensure_empty_output_directory(output_dir: Path) -> None:
    if output_dir.exists() and any(output_dir.iterdir()):
        raise RuntimeError(f"Output directory must be empty: {output_dir}")
    output_dir.mkdir(parents=True, exist_ok=True)


def pull_listing(package_name: str, token: str) -> tuple[list[dict[str, Any]], dict[str, dict[str, int]]]:
    edit = request_json("POST", api_url(package_name, "edits"), token=token, body=b"{}", content_type="application/json")
    edit_id = edit.get("id")
    if not edit_id:
        raise RuntimeError("Google Play API did not return an edit ID")
    try:
        listing_response = request_json(
            "GET",
            api_url(package_name, f"edits/{quote(edit_id, safe='')}/listings"),
            token=token,
        )
        listings = sorted(listing_response.get("listings", []), key=lambda listing: listing["language"])
        image_counts = {listing["language"]: {} for listing in listings}
        with ThreadPoolExecutor(max_workers=8) as executor:
            image_requests = {
                (language, image_type): executor.submit(
                    request_json,
                    "GET",
                    api_url(
                        package_name,
                        "edits/"
                        f"{quote(edit_id, safe='')}/listings/{quote(language, safe='')}/"
                        f"{quote(image_type, safe='')}",
                    ),
                    token=token,
                )
                for language in image_counts
                for image_type in IMAGE_TYPES
            }
            for (language, image_type), future in image_requests.items():
                image_counts[language][image_type] = len(future.result().get("images", []))
        return listings, image_counts
    finally:
        request_json("DELETE", api_url(package_name, f"edits/{quote(edit_id, safe='')}"), token=token)


def write_output(
    output_dir: Path,
    package_name: str,
    listings: list[dict[str, Any]],
    image_counts: dict[str, dict[str, int]],
) -> None:
    generated_at = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    (output_dir / "listings.json").write_text(
        json.dumps(
            {
                "generatedAt": generated_at,
                "source": "google-play-publishing-api",
                "packageName": package_name,
                "listings": listings,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    (output_dir / "image-counts.json").write_text(
        json.dumps(
            {
                "generatedAt": generated_at,
                "source": "google-play-publishing-api",
                "packageName": package_name,
                "imageCounts": image_counts,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def main() -> int:
    args = parse_args()
    if not args.credentials:
        raise RuntimeError("Pass --credentials or set FEEDFLOW_PLAY_CONFIG_JSON")
    credentials_path = Path(args.credentials)
    if not credentials_path.is_file():
        raise RuntimeError(f"Service-account JSON does not exist: {credentials_path}")
    ensure_empty_output_directory(args.output_dir)
    token = access_token(json.loads(credentials_path.read_text(encoding="utf-8")))
    listings, image_counts = pull_listing(args.package_name, token)
    write_output(args.output_dir, args.package_name, listings, image_counts)
    print(f"Wrote {len(listings)} listings and image counts to {args.output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
