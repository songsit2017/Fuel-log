#!/usr/bin/env python3
"""Build a same-origin oil-price file from official Bangchak, PTT, Shell, PT, and Caltex sources."""
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
# PT (พีที) official price endpoint – documented at https://ptmax.th
PT_URL = "https://www.ptmaxoil.com/th/oil-price"
# Caltex (คาลเท็กซ์) – Chevron Thailand official price page
CALTEX_URL = "https://www.caltexthailand.com/th/retail/products/fuels/oil-prices.html"
OUT = Path(__file__).resolve().parents[1] / "oil-prices.json"
HEADERS = {"User-Agent": "FuelLog-Pro/9.0.0 (+GitHub Actions)"}


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
        "e20": prices.get("gasohol e20") or prices.get("e20"),
        "e85": prices.get("gasohol e85") or prices.get("e85"),
        "diesel_b7": prices.get("diesel") or prices.get("hi diesel") or prices.get("hi-diesel"),
        "diesel_b20": prices.get("diesel b20") or prices.get("b20"),
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
        "e20": positive(prices.get("e20") or prices.get("e20_gasoline")),
        "e85": positive(prices.get("e85") or prices.get("e85_gasoline")),
        "diesel_b7": positive(prices.get("fuelsave_regular_diesel") or prices.get("regular_diesel")),
        "diesel_b20": positive(prices.get("b20") or prices.get("b20_diesel")),
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


def fetch_pt():
    """Scrape PT (พีที) official price page for grade prices."""
    page = read_url(PT_URL).decode("utf-8", "replace")
    # PT page typically lists prices in a table with product names and prices
    prices = {}
    # Look for common price patterns: digit(s).digit(s) near fuel grade keywords
    for pattern, key in [
        (r"(?:แก๊สโซฮอล์\s*95|gasohol\s*95)[^\d]*(\d{2,3}\.\d{2})", "gasohol_95"),
        (r"(?:แก๊สโซฮอล์\s*91|gasohol\s*91)[^\d]*(\d{2,3}\.\d{2})", "gasohol_91"),
        (r"(?:E20|แก๊สโซฮอล์\s*E20)[^\d]*(\d{2,3}\.\d{2})", "e20"),
        (r"(?:E85|แก๊สโซฮอล์\s*E85)[^\d]*(\d{2,3}\.\d{2})", "e85"),
        (r"(?:ดีเซล\s*B7|diesel\s*b7|Hi-Diesel)[^\d]*(\d{2,3}\.\d{2})", "diesel_b7"),
        (r"(?:ดีเซล\s*B20|diesel\s*b20)[^\d]*(\d{2,3}\.\d{2})", "diesel_b20"),
    ]:
        m = re.search(pattern, page, re.IGNORECASE)
        if m:
            prices[key] = positive(m.group(1))
    if not any(prices.values()):
        raise ValueError("PT price page returned no recognized prices")
    return {
        "source": PT_URL,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
        "grades": prices,
    }


def fetch_caltex():
    """Scrape Caltex (Chevron Thailand) official price page."""
    page = read_url(CALTEX_URL).decode("utf-8", "replace")
    prices = {}
    for pattern, key in [
        (r"(?:Techron\s*95|แก๊สโซฮอล์\s*95)[^\d]*(\d{2,3}\.\d{2})", "gasohol_95"),
        (r"(?:Techron\s*91|แก๊สโซฮอล์\s*91)[^\d]*(\d{2,3}\.\d{2})", "gasohol_91"),
        (r"(?:E20)[^\d]*(\d{2,3}\.\d{2})", "e20"),
        (r"(?:E85)[^\d]*(\d{2,3}\.\d{2})", "e85"),
        (r"(?:Diesel\s*B7|ดีเซล\s*B7|Hi-Diesel)[^\d]*(\d{2,3}\.\d{2})", "diesel_b7"),
        (r"(?:Diesel\s*B20|ดีเซล\s*B20)[^\d]*(\d{2,3}\.\d{2})", "diesel_b20"),
    ]:
        m = re.search(pattern, page, re.IGNORECASE)
        if m:
            prices[key] = positive(m.group(1))
    if not any(prices.values()):
        raise ValueError("Caltex price page returned no recognized prices")
    return {
        "source": CALTEX_URL,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
        "grades": prices,
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
    fetchers = [
        ("ptt", fetch_ptt),
        ("shell", fetch_shell),
        ("pt", fetch_pt),
        ("caltex", fetch_caltex),
    ]
    for key, fetcher in fetchers:
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
