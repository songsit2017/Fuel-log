#!/usr/bin/env python3
"""Fetch Bangchak's official oil-price JSON and write a same-origin file for GitHub Pages."""
from __future__ import annotations
import json
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

URL = "https://oil-price.bangchak.co.th/ApiOilPrice2/th"
OUT = Path(__file__).resolve().parents[1] / "oil-prices.json"

req = urllib.request.Request(URL, headers={"User-Agent": "FuelLog-Pro/3.7 (+GitHub Actions)", "Accept": "application/json"})
try:
    with urllib.request.urlopen(req, timeout=30) as res:
        raw = res.read().decode("utf-8-sig")
    data = json.loads(raw)
    head = data[0] if isinstance(data, list) and data else data
    if not isinstance(head, dict):
        raise ValueError("unexpected root payload")
    oil_list = head.get("OilList") or head.get("oilList") or head.get("list")
    if isinstance(oil_list, str):
        oil_list = json.loads(oil_list)
    if not isinstance(oil_list, list) or not oil_list:
        raise ValueError("OilList missing or empty")
    payload = {
        "fetchedAt": datetime.now(timezone.utc).isoformat(),
        "source": URL,
        "data": data,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"updated {OUT} ({len(oil_list)} products)")
except Exception as exc:
    print(f"oil price update failed: {exc}", file=sys.stderr)
    # Preserve a valid previous file rather than replacing it with an error.
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text(encoding="utf-8"))
            if old.get("data"):
                print("keeping previous oil-prices.json")
                sys.exit(0)
        except Exception:
            pass
    raise
