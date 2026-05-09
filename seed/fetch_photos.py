"""
For the top-N places (by rating × sqrt(review count)) in places-from-api.json,
hit get_place_details_tool to pull photo URLs, save to places-photo-urls.json
keyed by Google place_id. enrich_from_api.py will merge these on next run.
"""
import json, os, sys, urllib.request, time, math

URL = os.environ.get("MCP_URL", "https://origin-qa-mcp-jiocreate.jiomedia.net/mcp")
TOK = os.environ["MCP_TOKEN"]
TOP_N = int(os.environ.get("TOP_N", "50"))
PHOTOS_PER_PLACE = 3

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(ROOT, "places-from-api.json")
OUT  = os.path.join(ROOT, "places-photo-urls.json")

def post(body, sid=None):
    h = {"Authorization": f"Bearer {TOK}", "Content-Type": "application/json",
         "Accept": "application/json, text/event-stream"}
    if sid: h["Mcp-Session-Id"] = sid
    req = urllib.request.Request(URL, data=json.dumps(body).encode(), headers=h, method="POST")
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.headers.get("Mcp-Session-Id"), r.read().decode()

def parse(raw):
    if not raw.strip(): return None
    for line in raw.splitlines():
        if line.startswith("data: "): return json.loads(line[6:])
    return json.loads(raw)

# Init session
sid, _ = post({"jsonrpc":"2.0","id":1,"method":"initialize",
    "params":{"protocolVersion":"2024-11-05","capabilities":{},
              "clientInfo":{"name":"wo-photos","version":"0.1"}}})
post({"jsonrpc":"2.0","method":"notifications/initialized"}, sid)
print(f"session: {sid}", file=sys.stderr)

with open(SRC) as f:
    places = json.load(f)["places"]

def score(p):
    r = p.get("rating") or 4.0
    n = p.get("userRatingCount") or 100
    return r * math.sqrt(n)

ranked = sorted(places, key=score, reverse=True)[:TOP_N]
print(f"fetching photos for top {len(ranked)} of {len(places)}", file=sys.stderr)

# Carry over any prior fetch results (idempotent)
existing = {}
if os.path.exists(OUT):
    with open(OUT) as f:
        existing = json.load(f)

photos_by_id = dict(existing)
ok = skip = fail = 0

for i, p in enumerate(ranked, 1):
    pid = p["id"]
    if pid in photos_by_id and photos_by_id[pid]:
        skip += 1
        continue
    try:
        _, raw = post({"jsonrpc":"2.0","id":100+i,"method":"tools/call",
            "params":{"name":"get_place_details_tool","arguments":{
                "user_id":5,"req":{"operation_id":f"photo-{i}","place_id":pid}}}}, sid)
        resp = parse(raw)
        if not resp or "result" not in resp:
            raise RuntimeError(f"no result: {str(resp)[:200]}")
        sc = resp["result"].get("structuredContent") or {}
        biz = sc.get("business_info") or {}
        photos = biz.get("photos") or []
        urls = [ph.get("imageUrl") for ph in photos if ph.get("imageUrl")]
        photos_by_id[pid] = urls[:PHOTOS_PER_PLACE]
        ok += 1
        if i % 10 == 0:
            print(f"  {i}/{len(ranked)}  {p['displayName'][:40]}  ({len(urls)} photos)", file=sys.stderr)
    except Exception as e:
        fail += 1
        photos_by_id[pid] = []
        print(f"  ! {pid} {p['displayName'][:40]}: {e}", file=sys.stderr)
    time.sleep(0.25)

with open(OUT, "w") as f:
    json.dump(photos_by_id, f, indent=2)

print(f"\nfetched: {ok}, cached-skip: {skip}, failed: {fail}")
print(f"output:  {OUT}")
print(f"unique places with photos: {sum(1 for v in photos_by_id.values() if v)}")
