"""Transport regression tests; no devices, credentials or provider requests."""

import argparse
import importlib.util
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location("controller", Path(__file__).parents[1] / "controller.py")
controller = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(controller)


class ControllerTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "commands").mkdir()
        self.manifest = {
            "run_id": "device1-run", "device_id": "device1",
            "runner_bundle_id": "example.controller.UITests.xctrunner",
        }
        controller.write_json(self.root / "run.json", self.manifest)
        self.pending = {"id": "command1", "runID": "device1-run", "action": "tap", "target": "Delete feed"}
        self.output = StringIO()
        self.output_patch = redirect_stdout(self.output)
        self.output_patch.__enter__()
        self.addCleanup(self.output_patch.__exit__, None, None, None)

    def write_pending(self):
        controller.write_json(self.root / "pending.json", self.pending)

    def respond(self, **fields):
        response = {"id": "command1", "runID": "device1-run", "ok": True, "tree": "Verified UI"}
        response.update(fields)
        controller.write_json(self.root / "incoming.json", response)

    def test_transfer_timeout_keeps_uncertainty_and_does_not_resend(self):
        args = argparse.Namespace(action="tap", target="Delete feed", text=None, timeout_seconds=1)
        with patch.object(controller, "transfer", side_effect=controller.ControllerError("timed out")) as transfer:
            with self.assertRaisesRegex(controller.ControllerError, "timed out"):
                controller.send(args, self.root, self.manifest)
            self.assertEqual(transfer.call_count, 1)
            self.assertTrue((self.root / "pending.json").exists())
            with self.assertRaisesRegex(controller.ControllerError, "unresolved"):
                controller.send(args, self.root, self.manifest)
            self.assertEqual(transfer.call_count, 1)

    def test_collect_ignores_another_run_and_command_then_acknowledges(self):
        self.write_pending()
        replies = [{"runID": "device2-run"}, {"id": "older-command"}, {}]

        def transfer(*args):
            self.assertEqual(args[1], "from")
            self.respond(**replies.pop(0))

        with patch.object(controller, "transfer", side_effect=transfer), patch.object(controller.time, "sleep"):
            result = controller.collect(self.root, self.manifest, 5)
        self.assertTrue(result["ok"])
        self.assertFalse((self.root / "pending.json").exists())
        self.assertEqual((self.root / "commands/command1.response.txt").read_text(), "Verified UI\n")

    def test_known_ui_failure_is_saved_and_is_not_success(self):
        self.write_pending()
        with patch.object(controller, "transfer", side_effect=lambda *args: self.respond(ok=False, error="Not hittable")):
            with self.assertRaisesRegex(controller.ControllerError, "Not hittable"):
                controller.collect(self.root, self.manifest, 1)
        self.assertFalse((self.root / "pending.json").exists())
        result = json.loads((self.root / "commands/command1.response.json").read_text())
        self.assertFalse(result["ok"])

    def test_no_response_retains_pending_and_reports_last_transport_error(self):
        self.write_pending()
        with patch.object(controller, "transfer", side_effect=controller.ControllerError("Device disconnected")), \
                patch.object(controller.time, "monotonic", side_effect=[0, 0, 0, 2, 2]), \
                patch.object(controller.time, "sleep"):
            with self.assertRaisesRegex(controller.ControllerError, "Device disconnected.*may have run"):
                controller.collect(self.root, self.manifest, 1)
        self.assertTrue((self.root / "pending.json").exists())

    def test_concurrent_sender_cannot_overwrite_active_command(self):
        with controller.session(self.root):
            with self.assertRaisesRegex(controller.ControllerError, "Another command is active"):
                with controller.session(self.root):
                    self.fail("Second actor acquired the same run")

    def test_screenshot_is_collected_only_for_the_matched_response(self):
        self.write_pending()

        def transfer(manifest, direction, source, destination, timeout):
            self.assertEqual(direction, "from")
            if source.endswith("response.json"):
                self.respond(screenshot="command1.png")
            else:
                self.assertEqual(source, "Documents/device1-run/command1.png")
                Path(destination).write_bytes(b"image bytes")

        with patch.object(controller, "transfer", side_effect=transfer):
            controller.collect(self.root, self.manifest, 1)
        self.assertEqual((self.root / "commands/command1.png").read_bytes(), b"image bytes")

    def test_transfer_uses_integral_seconds_near_the_deadline(self):
        for remaining, expected in [(14.7, 15), (0.25, 1)]:
            with self.subTest(remaining=remaining), patch.object(controller, "run_command") as command:
                controller.transfer(self.manifest, "from", "response.json", "local.json", remaining)
                arguments, process_timeout = command.call_args.args
                self.assertEqual(arguments[arguments.index("--timeout") + 1], str(expected))
                self.assertEqual(process_timeout, expected + 2)

    def test_abandon_archives_unknown_outcome_without_dispatch(self):
        self.write_pending()
        with patch.object(controller, "transfer") as transfer:
            self.assertEqual(controller.main(["abandon", "--run-dir", str(self.root)]), 0)
        transfer.assert_not_called()
        self.assertFalse((self.root / "pending.json").exists())
        self.assertEqual(json.loads((self.root / "commands/command1.abandoned.json").read_text()), self.pending)


if __name__ == "__main__":
    unittest.main()
