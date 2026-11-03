import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("verify_apk", Path(__file__).resolve().parents[1] / "verify-shield-apk.py")
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class CertificateOutputTests(unittest.TestCase):
    def test_numbered_signer_format(self):
        text = f"Signer #1 certificate SHA-256 digest: {verifier.EXPECTED_CERT}"
        self.assertEqual({verifier.EXPECTED_CERT}, verifier.certificate_fingerprints(text))

    def test_actual_github_v2_format(self):
        text = f"V2 Signer: certificate SHA-256 digest: {verifier.EXPECTED_CERT}"
        self.assertEqual({verifier.EXPECTED_CERT}, verifier.certificate_fingerprints(text))

    def test_same_certificate_across_schemes(self):
        text = "\n".join(f"{scheme} Signer: certificate SHA-256 digest: {verifier.EXPECTED_CERT}"
                         for scheme in ["V2", "V3", "V3.1"])
        self.assertEqual({verifier.EXPECTED_CERT}, verifier.certificate_fingerprints(text))

    def test_different_or_multiple_signers_do_not_match(self):
        different = "0" * 64
        text = f"Signer #1 certificate SHA-256 digest: {different}"
        self.assertNotEqual({verifier.EXPECTED_CERT}, verifier.certificate_fingerprints(text))
        text += f"\nSigner #2 certificate SHA-256 digest: {verifier.EXPECTED_CERT}"
        self.assertNotEqual({verifier.EXPECTED_CERT}, verifier.certificate_fingerprints(text))

    def test_missing_or_malformed_digest_fails_closed(self):
        for text in ["", "Verified", "V2 Signer: certificate SHA-256 digest: abc"]:
            self.assertEqual(set(), verifier.certificate_fingerprints(text))


if __name__ == "__main__":
    unittest.main()
