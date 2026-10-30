#!/usr/bin/env python3
"""Serve the deterministic Maestro WAV fixture with byte-range support."""

from __future__ import annotations

import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


FIXTURE = Path(__file__).resolve().parents[1] / "fixtures/audio/feedflow-e2e-tone.wav"


class AudioFixtureHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_HEAD(self) -> None:
        self._serve_audio(send_body=False)

    def do_GET(self) -> None:
        self._serve_audio(send_body=True)

    def _serve_audio(self, *, send_body: bool) -> None:
        if self.path.split("?", 1)[0] != "/feedflow-e2e-tone.wav":
            self.send_error(404)
            return

        size = FIXTURE.stat().st_size
        range_header = self.headers.get("Range")
        start, end, status = 0, size - 1, 200
        if range_header:
            try:
                unit, span = range_header.split("=", 1)
                first, last = span.split("-", 1)
                if unit != "bytes":
                    raise ValueError
                if first:
                    start = int(first)
                    end = min(int(last), size - 1) if last else size - 1
                elif last:
                    suffix_length = int(last)
                    if suffix_length <= 0:
                        raise ValueError
                    start = max(size - suffix_length, 0)
                    end = size - 1
                else:
                    raise ValueError
                if start < 0 or start >= size or end < start:
                    raise ValueError
                status = 206
            except ValueError:
                self.send_response(416)
                self.send_header("Content-Range", f"bytes */{size}")
                self.send_header("Content-Length", "0")
                self.end_headers()
                return

        length = end - start + 1
        self.send_response(status)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(length))
        if status == 206:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        if send_body:
            with FIXTURE.open("rb") as fixture:
                fixture.seek(start)
                self.wfile.write(fixture.read(length))

    def log_message(self, format: str, *args: object) -> None:
        print(format % args, flush=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=0, help="Port to bind; 0 selects an ephemeral port")
    parser.add_argument("--port-file", type=Path, required=True, help="Write the selected port here")
    args = parser.parse_args()

    server = ThreadingHTTPServer(("127.0.0.1", args.port), AudioFixtureHandler)
    args.port_file.write_text(f"{server.server_address[1]}\n", encoding="ascii")
    print(f"Serving {FIXTURE.name} on port {server.server_address[1]}", flush=True)
    try:
        server.serve_forever()
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
