#!/usr/bin/env python3
"""Build a same-origin oil-price file from official Bangchak, OR and Shell sources."""
from __future__ import annotations

import html
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

BANGCHAK_URL = "https://oil-price.bangchak.co.th/ApiOilPrice2/th"
PTT_URL = "https://orapiweb.pttor.com/oilservice/OilPrice.asmx"
SHELL_URL = "https://find.shell.com/th/fuel/10041865-co-wutthipan-b1-sai-mai-31-bkk/en_TH"
OUT = Path(__file__).resolve().parents[1] / "oil-prices.json"
HEADERS = {"User-Agent": "FuelLog-Pro/7.1.1 (+GitHub Actions)"}


def read_url(url: str, *, data: bytes | None = None, headers: dict | None = None) -> bytes:
    req = urllib.request.Request(url, data=data, headers={**HEADERS, **(headers or {})})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.read()


def positive(value):
    try:
        number = float(value)
        return number if number > 0 else None
    except (TypeError, ValueError):
        return None


def fetch_bangchak():
    data = json.loads(read_url(BANGCHAK_URL, headers={"Accept": "application/json"}).decode("utf-8-sig"))
    head = data[0] if isinstance(data, list) and data else data
    oil_list = head.get("OilList") or head.get("oilList") or head.get("list")
    if isinstance(oil_list, str):
        oil_list = json.loads(oil_list)
    if not isinstance(oil_list, list) or not oil_list:
        raise ValueError("Bangchak OilList missing or empty")
    return data


def fetch_ptt():
    now = datetime.now()
    envelope = f"""<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <GetOilPrice xmlns="http://www.pttor.com">
      <Language>EN</Language><DD>{now.day}</DD><MM>{now.month}</MM><YYYY>{now.year}</YYYY>
    </GetOilPrice>
  </soap:Body>
</soap:Envelope>""".encode()
    raw = read_url(
        PTT_URL,
        data=envelope,
        headers={
            "Content-Type": "text/xml; charset=utf-8",
            "SOAPAction": '"https://orapiweb.pttor.com/GetOilPrice"',
        },
    )
    outer = ET.fromstring(raw)
    result = next((node.text for node in outer.iter() if node.tag.endswith("GetOilPriceResult")), None)
    if not result:
        raise ValueError("OR GetOilPriceResult missing")
    fuels = ET.fromstring(result).findall(".//FUEL")
    prices = {str(f.findtext("PRODUCT") or "").strip().lower(): positive(f.findtext("PRICE")) for f in fuels}
    grades = {
        "gasohol_95": prices.get("gasohol 95"),
        "gasohol_91": prices.get("gasohol 91"),
        "diesel_b7": prices.get("diesel"),
        "premium_95": prices.get("super power gsh95"),
    }
    if not any(grades.values()):
        raise ValueError("OR returned no recognized prices")
    return {
        "source": PTT_URL,
        "updatedAt": fuels[0].findtext("PRICE_DATE") if fuels else None,
        "grades": grades,
    }


def fetch_shell():
    page = read_url(SHELL_URL).decode("utf-8", "replace")
    match = re.search(r'<script[^>]+data-page="app"[^>]*>(.*?)</script>', page, re.S)
    if not match:
        raise ValueError("Shell station data missing")
    data = json.loads(html.unescape(match.group(1)))
    location = data["props"]["location"]
    pricing = location.get("fuel_pricing") or {}
    prices = pricing.get("prices") or {}
    grades = {
        "gasohol_95": positive(prices.get("midgrade_gasoline")),
        "gasohol_91": positive(prices.get("low_octane_gasoline")),
        "diesel_b7": positive(prices.get("fuelsave_regular_diesel")),
        "premium_95": positive(prices.get("premium_gasoline")),
    }
    if not any(grades.values()):
        raise ValueError("Shell station returned no recognized prices")
    return {
        "source": SHELL_URL,
        "updatedAt": pricing.get("updated"),
        "station": location.get("name"),
        "grades": grades,
    }


def old_comparison():
    try:
        previous = json.loads(OUT.read_text(encoding="utf-8"))
        return previous.get("comparison") or {}
    except Exception:
        return {}


def main():
    previous = old_comparison()
    bangchak = fetch_bangchak()
    comparison = {}
    for key, fetcher in (("ptt", fetch_ptt), ("shell", fetch_shell)):
        try:
            comparison[key] = fetcher()
            print(f"updated {key} official prices")
        except Exception as exc:
            print(f"{key} price update failed: {exc}", file=sys.stderr)
            if previous.get(key):
                comparison[key] = previous[key]
                print(f"keeping previous {key} prices")
    payload = {
        "fetchedAt": datetime.now(timezone.utc).isoformat(),
        "source": BANGCHAK_URL,
        "data": bangchak,
        "comparison": comparison,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"updated {OUT}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"oil price update failed: {exc}", file=sys.stderr)
        if OUT.exists():
            try:
                if json.loads(OUT.read_text(encoding="utf-8")).get("data"):
                    print("keeping previous oil-prices.json")
                    sys.exit(0)
            except Exception:
                pass
        raise
