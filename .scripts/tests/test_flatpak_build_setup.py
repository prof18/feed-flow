from pathlib import Path
import os
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "flatpak-build-setup.sh"


class FlatpakBuildSetupTests(unittest.TestCase):
    def run_setup(self, shared_version="21.0.11", desktop_version="25.0.4.1", missing=None, version="MAJOR=1\nMINOR=19\nPATCH=0\n"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            sdk = root / "sdk"
            for major, compiler_version in [(21, shared_version), (25, desktop_version)]:
                if missing == major:
                    continue
                compiler = sdk / f"openjdk{major}/jvm/openjdk-{major}/bin/javac"
                compiler.parent.mkdir(parents=True)
                compiler.write_text(f"#!/bin/sh\nprintf 'javac {compiler_version}\\n'\n")
                compiler.chmod(0o755)
            resource = root / "desktopApp/src/jvmMain/resources"
            resource.mkdir(parents=True)
            props = resource / "props.properties"
            props.write_text("existing=true\n")
            wrapper = root / "gradle/wrapper/gradle-wrapper.properties"
            wrapper.parent.mkdir(parents=True)
            wrapper.write_text("distributionUrl=https://example.invalid/gradle.zip\n")
            (root / "build.gradle.kts").write_text(
                "alias(libs.plugins.android.application) apply false\n"
                "vendor = JvmVendorSpec.JETBRAINS\n"
            )
            (root / "gradle.properties").write_text("existing=true\n")
            (root / "version.properties").write_text(version)
            env = os.environ.copy()
            env["FEEDFLOW_FLATPAK_SDK_ROOT"] = str(sdk)
            # A valid PATH compiler must not hide a missing/wrong SDK compiler.
            host = root / "host-bin"
            host.mkdir()
            (host / "javac").write_text("#!/bin/sh\nprintf 'javac 25.0.4.1\\n'\n")
            (host / "javac").chmod(0o755)
            env["PATH"] = str(host) + os.pathsep + env["PATH"]
            before = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob("*") if p.is_file()}
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True)
            after = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob("*") if p.is_file()}
            return result, before, after, str(sdk)

    def test_missing_shared_jdk_fails_before_mutation(self):
        result, before, after, _ = self.run_setup(missing=21)
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, after)

    def test_wrong_shared_jdk_fails_before_mutation(self):
        result, before, after, _ = self.run_setup(shared_version="17.0.10")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, after)

    def test_missing_desktop_jdk_fails_before_mutation(self):
        result, before, after, _ = self.run_setup(missing=25)
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, after)

    def test_wrong_desktop_jdk_fails_before_mutation(self):
        result, before, after, _ = self.run_setup(desktop_version="21.0.11")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, after)

    def test_invalid_version_fails_before_mutation(self):
        result, before, after, _ = self.run_setup(version="MAJOR=1\nMINOR=19\nPATCH=invalid\n")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, after)

    def test_complete_sdk_uses_current_version_and_both_toolchains(self):
        result, _, after, sdk = self.run_setup()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        props = after["desktopApp/src/jvmMain/resources/props.properties"].decode()
        self.assertIn("version=1.19.0\n", props)
        self.assertIn("flatpak=true\n", props)
        self.assertIn("is_release=true\n", props)
        self.assertNotIn("version=1.18.0", props)
        gradle = after["gradle.properties"].decode()
        self.assertIn(f"{sdk}/openjdk21/jvm/openjdk-21", gradle)
        self.assertIn(f"{sdk}/openjdk25/jvm/openjdk-25", gradle)
        self.assertIn("org.gradle.java.installations.auto-download=false", gradle)


if __name__ == "__main__":
    unittest.main()
