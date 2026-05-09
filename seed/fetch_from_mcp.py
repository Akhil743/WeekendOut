"""
One-shot: probe a handful of geographies/types via the JioCreate MCP and write
~100+ Google Places into seed/places-from-api.json. Run with:
    MCP_TOKEN=... MCP_URL=https://... python3 /tmp/wo_mcp_fetch.py
"""
import json, os, sys, urllib.request, time, uuid

TOKEN = os.environ["MCP_TOKEN"]
URL   = os.environ.get("MCP_URL", "https://origin-qa-mcp-jiocreate.jiomedia.net/mcp")
USER_ID = 5
OUT = os.path.expanduser("~/Akhil-Personal/weekendout/seed/places-from-api.json")

QUERIES = [
    # (label, lat, lng, radius_m, types)
    ("Indiranagar cafes",      12.9719, 77.6412, 1500,  ["cafe"]),
    ("Indiranagar bars",       12.9719, 77.6412, 1500,  ["bar"]),
    ("Koramangala cafes",      12.9352, 77.6245, 1500,  ["cafe"]),
    ("Koramangala restaurants",12.9352, 77.6245, 1500,  ["restaurant"]),
    ("HSR Layout cafes",       12.9082, 77.6476, 2000,  ["cafe"]),
    ("Whitefield brewpubs",    12.9698, 77.7500, 3000,  ["bar"]),
    ("Central BLR attractions",12.9716, 77.5946, 4000,  ["tourist_attraction","park"]),
    ("Coorg homestays",        12.4244, 75.7382, 15000, ["lodging"]),
    ("Chikmagalur stays",      13.3161, 75.7720, 20000, ["lodging"]),
    ("Mysuru attractions",     12.2958, 76.6394, 6000,  ["tourist_attraction"]),
    ("Nandi Hills area",       13.3702, 77.6835, 10000, ["tourist_attraction","park"]),
    ("Sakleshpur stays",       12.9407, 75.7866, 15000, ["lodging"]),
]

def post(body, session_id=None):
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    }
    if session_id:
        headers["Mcp-Session-Id"] = session_id
    req = urllib.request.Request(URL, data=json.dumps(body).encode(), headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=60) as resp:
        sid = resp.headers.get("Mcp-Session-Id")
        raw = resp.read().decode()
    # Notifications have empty bodies (HTTP 202)
    if not raw.strip():
        return sid, None
    # SSE -> json
    payload = None
    for line in raw.splitlines():
        if line.startswith("data: "):
            payload = json.loads(line[6:])
            break
    if payload is None:
        payload = json.loads(raw)
    return sid, payload

# Init
sid, init_resp = post({
    "jsonrpc":"2.0","id":1,"method":"initialize",
    "params":{"protocolVersion":"2024-11-05","capabilities":{},
              "clientInfo":{"name":"weekendout-seed","version":"0.1.0"}}
})
print(f"session: {sid}")

# Initialized notification
post({"jsonrpc":"2.0","method":"notifications/initialized"}, sid)

aggregated = []
seen = set()

for label, lat, lng, radius, types in QUERIES:
    op_id = f"wo-{uuid.uuid4().hex[:8]}"
    body = {
        "jsonrpc":"2.0","id":hash(op_id) & 0x7fffffff,
        "method":"tools/call",
        "params":{
            "name":"get_nearby_places_tool",
            "arguments":{
                "user_id": USER_ID,
                "req":{
                    "operation_id": op_id,
                    "latitude": lat, "longitude": lng,
                    "radius": radius, "included_types": types,
                },
            }
        }
    }
    try:
        _, resp = post(body, sid)
        sc = resp.get("result", {}).get("structuredContent") or {}
        places = sc.get("places", []) or []
    except Exception as e:
        print(f"  ! {label}: {e}", file=sys.stderr)
        continue

    new = 0
    for p in places:
        pid = p.get("id")
        if not pid or pid in seen:
            continue
        seen.add(pid)
        p["_query_label"] = label
        aggregated.append(p)
        new += 1
    print(f"  {label}: {len(places)} returned, {new} new (total: {len(aggregated)})")
    time.sleep(0.3)  # be polite

with open(OUT, "w") as f:
    json.dump({"queries": [q[0] for q in QUERIES], "places": aggregated}, f, indent=2)
print(f"\nWrote {len(aggregated)} unique places -> {OUT}")
