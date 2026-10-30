import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import update_oil_price as updater


class UpdatePricesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.output = Path(self.temp.name) / "oil-prices.json"
        self.previous = {
            "fetchedAt": "old-check",
            "source": updater.BANGCHAK_URL,
            "data": [{"OilRemark2": "Effective today", "OilList": "prices"}],
            "comparison": {
                key: {"source": key, "updatedAt": "old-date", "grades": {"e20": 35.69}}
                for key in ("ptt", "shell", "pt", "caltex")
            },
        }
        self.output.write_text(json.dumps(self.previous), encoding="utf-8")
        self.before = self.output.read_bytes()
        self.next = copy.deepcopy(self.previous)
        for key in ("shell", "pt", "caltex"):
            self.next["comparison"][key]["updatedAt"] = "new-check"

    def run_update(self, failed=()):
        with patch.object(updater, "OUT", self.output), patch.dict(updater.os.environ, {"GITHUB_STEP_SUMMARY": ""}):
            with patch.object(updater, "fetch_bangchak", return_value=self.next["data"]):
                from contextlib import ExitStack
                with ExitStack() as stack:
                    for key in ("ptt", "shell", "pt", "caltex"):
                        kwargs = {"side_effect": ValueError("offline")} if key in failed else {
                            "return_value": self.next["comparison"][key]
                        }
                        stack.enter_context(patch.object(updater, "fetch_" + key, **kwargs))
                    updater.main()
        return json.loads(self.output.read_text(encoding="utf-8"))

    def test_check_times_do_not_rewrite_file(self):
        self.run_update()
        self.assertEqual(self.before, self.output.read_bytes())

    def test_each_brand_price_change_is_written(self):
        for key in ("ptt", "shell", "pt", "caltex"):
            with self.subTest(brand=key):
                self.setUp()
                self.next["comparison"][key]["grades"]["e20"] = 36.29
                result = self.run_update()
                self.assertEqual(36.29, result["comparison"][key]["grades"]["e20"])
                if key != "shell":
                    self.assertEqual("old-date", result["comparison"]["shell"]["updatedAt"])

    def test_effective_dates_are_not_ignored(self):
        self.next["comparison"]["ptt"]["updatedAt"] = "new-effective-date"
        self.next["data"][0]["OilRemark2"] = "Effective tomorrow"
        result = self.run_update()
        self.assertEqual(self.next["data"], result["data"])
        self.assertEqual("new-effective-date", result["comparison"]["ptt"]["updatedAt"])

    def test_failed_source_keeps_previous_data_without_commit(self):
        self.run_update(failed=("shell",))
        self.assertEqual(self.before, self.output.read_bytes())

    def test_new_brand_and_grade_removal_are_written(self):
        del self.previous["comparison"]["caltex"]
        self.output.write_text(json.dumps(self.previous), encoding="utf-8")
        self.next["comparison"]["shell"]["grades"]["e20"] = None
        result = self.run_update()
        self.assertIn("caltex", result["comparison"])
        self.assertIsNone(result["comparison"]["shell"]["grades"]["e20"])

    def test_calendar_day_does_not_rewrite_file(self):
        self.next["data"][0]["OilDateNow"] = "30/10/2569"
        self.run_update()
        self.assertEqual(self.before, self.output.read_bytes())

    def test_bangchak_price_and_effective_date_are_significant(self):
        for field in ("OilList", "OilPriceDate", "OilPriceTime"):
            with self.subTest(field=field):
                self.setUp()
                self.next["data"][0][field] = "changed"
                self.assertEqual("changed", self.run_update()["data"][0][field])

    def test_ptt_requests_thai_date(self):
        from datetime import datetime, timezone
        class Clock(datetime):
            @classmethod
            def now(cls, tz=None):
                return datetime(2026, 10, 29, 21, 45, tzinfo=timezone.utc).astimezone(tz)
        with patch.object(updater, "datetime", Clock), patch.object(updater, "read_url", return_value=b"<root/>") as read:
            with self.assertRaises(ValueError):
                updater.fetch_ptt()
        self.assertIn(b"<DD>30</DD><MM>10</MM><YYYY>2026</YYYY>", read.call_args.kwargs["data"])

    def test_missing_file_is_created(self):
        self.output.unlink()
        self.assertEqual(self.next["comparison"]["ptt"], self.run_update()["comparison"]["ptt"])


if __name__ == "__main__":
    unittest.main()
