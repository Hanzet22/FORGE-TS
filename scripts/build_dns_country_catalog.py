#!/usr/bin/env python3
"""Build the offline country DNS observation catalog from saved PublicDNS snapshots."""

from __future__ import annotations

import glob
import ipaddress
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data/dns-catalog/raw"
OUTPUT = ROOT / "app/src/main/assets/dns-catalog/countries.json"
SOURCE_DATE = "2026-11-11"
IP_CANDIDATE = re.compile(
    r"(?<![A-Za-z0-9])(?:\d{1,3}\.){3}\d{1,3}(?![A-Za-z0-9])|"
    r"(?<![A-Za-z0-9:])(?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4}(?![A-Za-z0-9:])"
)


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    manifest = {item["code"]: item for item in read_json(RAW / "country-manifest.json")["countries"]}
    observations: dict[str, dict[str, dict[str, str]]] = {}

    for name in sorted(glob.glob(str(RAW / "countries" / "*.json"))):
        for page in read_json(Path(name)).get("pages", []):
            code = page.get("code")
            if code not in manifest:
                continue
            for line in page.get("rawMarkdown", "").splitlines():
                if not line.lstrip().startswith("|"):
                    continue
                cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
                candidates = []
                for token in IP_CANDIDATE.findall(line):
                    try:
                        address = ipaddress.ip_address(token)
                        if address.is_global:
                            candidates.append(address.compressed)
                    except ValueError:
                        continue
                for address in candidates:
                    city = cells[1] if len(cells) > 1 else ""
                    reliability = ""
                    for cell in reversed(cells):
                        match = re.search(r"(\d{1,3}(?:\.\d+)?)\s*%", cell)
                        if match:
                            reliability = match.group(1)
                            break
                    previous = observations.setdefault(code, {}).get(address)
                    item = {"address": address, "city": city, "directoryReliabilityPercent": reliability}
                    if previous is None or (not previous["city"] and city):
                        observations[code][address] = item

    countries = []
    for code, addresses in sorted(observations.items(), key=lambda pair: manifest[pair[0]]["name"]):
        meta = manifest[code]
        ipv4 = sorted((item for item in addresses.values() if ":" not in item["address"]), key=lambda item: tuple(map(int, item["address"].split("."))))
        ipv6 = sorted((item for item in addresses.values() if ":" in item["address"]), key=lambda item: item["address"])
        if ipv4 or ipv6:
            countries.append({
                "code": code.upper(),
                "name": meta["name"],
                "sourceUrl": meta["url"],
                "ipv4": ipv4,
                "ipv6": ipv6,
                "verification": "PublicDNS.xyz directory observation; resolver access and safety not tested"
            })

    if len(countries) < 20:
        raise SystemExit(f"Expected at least 20 countries with public addresses, found {len(countries)}")
    if not any(country["ipv4"] for country in countries) or not any(country["ipv6"] for country in countries):
        raise SystemExit("The country catalog must include both IPv4 and IPv6 observations")

    payload = {
        "schemaVersion": 1,
        "source": "https://www.publicdns.xyz/country/",
        "capturedAt": SOURCE_DATE,
        "countryCount": len(countries),
        "addressCounts": {
            "ipv4": sum(len(country["ipv4"]) for country in countries),
            "ipv6": sum(len(country["ipv6"]) for country in countries),
        },
        "countries": countries,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(countries)} countries ({payload['addressCounts']['ipv4']} IPv4, {payload['addressCounts']['ipv6']} IPv6)")


if __name__ == "__main__":
    main()
