import importlib.util
import pathlib
import unittest


script = pathlib.Path(__file__).resolve().parents[1] / "generate-microsoft-store-listing.py"
spec = importlib.util.spec_from_file_location("generate_microsoft_store_listing", script)
listing = importlib.util.module_from_spec(spec)
spec.loader.exec_module(listing)


class KeywordLimitTests(unittest.TestCase):
    def test_word_limit_is_per_locale(self):
        self.assertEqual(listing.keyword_limit_problems("it", ["one two three"] * 7), [])
        problems = listing.keyword_limit_problems("it", ["one two three four"] * 7)
        self.assertIn("it: 28 keyword words, maximum is 21", problems)

    def test_keyword_count_and_length(self):
        self.assertIn("it: 8 keywords, maximum is 7", listing.keyword_limit_problems("it", ["word"] * 8))
        self.assertEqual(listing.keyword_limit_problems("it", ["x" * 40]), [])
        self.assertIn("maximum is 40", listing.keyword_limit_problems("it", ["x" * 41])[0])


if __name__ == "__main__":
    unittest.main()
