"""
Read places-from-api.json (raw Google Places data via the JioCreate MCP) and
emit places-enriched.csv in the schema seedPlaces.ts expects.

Curated fields (vibe, crowd, good_for, temp_class, duration_class, etc.) are
inferred from primaryType + rating + region. Inferred values are reasonable
defaults; override the top ~30 rows by hand and set tags_overridden=true in
Firestore so re-runs of seedPlaces.ts won't clobber your edits.
"""
import csv, json, math, os

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(ROOT, "places-from-api.json")
PHOTOS = os.path.join(ROOT, "places-photo-urls.json")
OUT  = os.path.join(ROOT, "places-enriched.csv")

BLR = (12.9716, 77.5946)

# ── Type → curated-field heuristics ────────────────────────────────────────
def vibe_for(p):
    pt = (p.get("primaryType") or "").lower()
    types = set((p.get("types") or []))
    if pt in {"cafe","coffee_shop","tea_house","bakery","pastry_shop","breakfast_restaurant"}:
        return ["quiet","retro"]
    if pt in {"bar","pub","brewpub","cocktail_bar","sports_bar","gastropub","brewery","night_club","lounge_bar"}:
        return ["lively"]
    if pt in {"resort_hotel","hotel","hostel","guest_house"}:
        return ["scenic","quiet"]
    if pt in {"tourist_attraction","scenic_spot","historical_place","historical_landmark"}:
        return ["scenic","retro"]
    if pt in {"park","botanical_garden","hiking_area","farm"}:
        return ["scenic","quiet"]
    if pt in {"zoo","amusement_park","aquarium","planetarium"}:
        return ["lively"]
    if pt in {"museum","art_museum","art_gallery"}:
        return ["retro","quiet"]
    if pt in {"hindu_temple","church"}:
        return ["quiet","retro"]
    if pt and "restaurant" in pt:
        return ["lively"]
    return ["lively"]

def crowd_for(p):
    n = p.get("userRatingCount", 0) or 0
    if n > 5000: return "high"
    if n > 1000: return "medium"
    return "low"

def good_for(p):
    pt = (p.get("primaryType") or "").lower()
    if pt in {"cafe","coffee_shop","tea_house","bakery"}:
        return ["couples","solo","groups"]
    if pt in {"bar","pub","brewpub","cocktail_bar","sports_bar","gastropub","brewery","night_club","lounge_bar"}:
        return ["groups","couples"]
    if pt in {"resort_hotel","hotel","hostel","guest_house"}:
        return ["couples","family","groups"]
    if pt in {"tourist_attraction","scenic_spot","historical_place","historical_landmark","museum","art_museum","art_gallery","zoo","amusement_park","aquarium","planetarium","hindu_temple","church"}:
        return ["family","couples","groups"]
    if pt in {"park","botanical_garden"}:
        return ["family","couples","solo"]
    if pt in {"hiking_area"}:
        return ["adventure","groups"]
    if pt and "restaurant" in pt:
        return ["couples","groups","family"]
    return ["couples","groups"]

def temp_class(query_label, address):
    a = (address or "").lower()
    if any(x in (query_label or "") for x in ("Coorg","Chikmagalur","Sakleshpur","Nandi","Ooty","Yelagiri")):
        return "cool"
    if "mysuru" in a or "mysore" in a:
        return "warm"
    return "temperate"

def has_stay(p):
    pt = (p.get("primaryType") or "").lower()
    types = set((p.get("types") or []))
    return pt in {"resort_hotel","hotel","hostel","guest_house"} or "lodging" in types

def duration_class(p, query_label):
    pt = (p.get("primaryType") or "").lower()
    if has_stay(p) and any(x in (query_label or "") for x in ("Coorg","Chikmagalur","Sakleshpur")):
        return "weekend"
    if pt in {"cafe","coffee_shop","tea_house","bakery","pastry_shop"}:
        return "few-hours"
    if pt in {"bar","pub","brewpub","cocktail_bar","sports_bar","gastropub","brewery","night_club","lounge_bar"}:
        return "half-day"
    if pt and "restaurant" in pt:
        return "half-day"
    if pt in {"tourist_attraction","scenic_spot","park","botanical_garden","museum","art_museum","art_gallery","historical_place","zoo","amusement_park","aquarium","planetarium","hiking_area"}:
        return "full-day"
    return "half-day"

def budget_for(p):
    # priceLevel is rare on places API; default sensibly by type
    pl = p.get("priceLevel")
    if isinstance(pl, str):
        return {"PRICE_LEVEL_FREE":1,"PRICE_LEVEL_INEXPENSIVE":1,"PRICE_LEVEL_MODERATE":2,
                "PRICE_LEVEL_EXPENSIVE":3,"PRICE_LEVEL_VERY_EXPENSIVE":4}.get(pl, 2)
    pt = (p.get("primaryType") or "").lower()
    if pt in {"resort_hotel","hotel"}: return 3
    if pt in {"hostel","guest_house"}: return 2
    if pt in {"brewpub","cocktail_bar"}: return 3
    return 2

def haversine_km(lat1, lng1, lat2, lng2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1))*math.cos(math.radians(lat2))*math.sin(dlng/2)**2
    return 2 * R * math.asin(math.sqrt(a))

def drive_min(lat, lng):
    if lat is None or lng is None: return 0
    km = haversine_km(BLR[0], BLR[1], lat, lng)
    if km < 8:    return 0
    if km < 60:   return int(km / 0.6)        # urban-ish: 36 km/h
    return int(km / 0.8)                      # highway: 48 km/h (matches BLR->Coorg/Chik real times)

def city_region(addr):
    """Cheap city/region extraction from a comma-formatted Indian address."""
    parts = [p.strip() for p in (addr or "").split(",")]
    # last 3 parts usually look like: ", City, State PIN, India"
    if len(parts) >= 3:
        city_part = parts[-3]
        state_part = parts[-2]
        # Strip pincodes from state_part
        state = " ".join(w for w in state_part.split() if not w.isdigit()).strip()
        return city_part, state
    return parts[-1] if parts else "", ""

def slug(s):
    out = []
    for c in s.lower():
        out.append(c if c.isalnum() else "-")
    s = "".join(out)
    while "--" in s:
        s = s.replace("--","-")
    return s.strip("-")[:60]

def short_summary(p):
    name = p.get("displayName") or "Unknown"
    rating = p.get("rating")
    rc = p.get("userRatingCount")
    pt = (p.get("primaryType") or "").replace("_"," ")
    bits = []
    bits.append(f"{pt.title() if pt else 'Place'} in {city_region(p.get('formattedAddress',''))[0]}")
    if rating and rc:
        bits.append(f"rated {rating}★ across {rc:,} Google reviews")
    return " — ".join(bits)[:200]

# ── Build CSV ──────────────────────────────────────────────────────────────
with open(SRC) as f:
    data = json.load(f)
places = data["places"]

photos_by_id = {}
if os.path.exists(PHOTOS):
    with open(PHOTOS) as f:
        photos_by_id = json.load(f)

with open(OUT, "w", newline="") as f:
    w = csv.writer(f)
    w.writerow([
        "place_id","name","city","region","address","lat","lng",
        "vibe","crowd","good_for","temp_class","has_stay","duration_class","budget",
        "season","drive_time_from_blr_min","photo_urls","short_summary","maps_query"
    ])
    used_ids = set()
    rows = 0
    for p in places:
        name = p.get("displayName") or "Unknown"
        addr = p.get("formattedAddress") or ""
        loc = p.get("location") or {}
        lat = loc.get("latitude")
        lng = loc.get("longitude")
        city, region = city_region(addr)
        pid = slug(name)
        # de-dupe slugs by appending google_id tail
        if pid in used_ids:
            tail = (p.get("id") or "")[-6:]
            pid = f"{pid}-{tail.lower()}"
        used_ids.add(pid)

        season = "|".join(["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"])

        w.writerow([
            pid,
            name,
            city,
            region,
            addr,
            lat or "",
            lng or "",
            "|".join(vibe_for(p)),
            crowd_for(p),
            "|".join(good_for(p)),
            temp_class(p.get("_query_label",""), addr),
            "true" if has_stay(p) else "false",
            duration_class(p, p.get("_query_label","")),
            budget_for(p),
            season,
            drive_min(lat, lng),
            "|".join(photos_by_id.get(p.get("id"), [])),
            short_summary(p),
            f"{name}, {city}".strip(", "),
        ])
        rows += 1

print(f"Wrote {rows} rows -> {OUT}")
