"""TEMPORARY probe. Prints the endpoint and token shape of each capture.

The contracts in the next PR are written from this output. It reports the
endpoint sequence, which drives `counts` and `order`, and whether each request
carries randomized_bundle_token, which is what separates a first install from a
returning device when the endpoints alone cannot.
"""
import json
import re
import sys

REQUEST = re.compile(r"posting to (\S+)")
BODY = re.compile(r"Post value = (\{.*)$")

for path in sys.argv[1:]:
    endpoints, tokens = [], []
    pending = None
    for line in open(path, errors="replace"):
        stripped = line.rstrip()
        match = REQUEST.search(stripped)
        if match:
            pending = match.group(1).split("branch.io")[-1]
            continue
        match = BODY.search(stripped)
        if match and pending is not None:
            try:
                body = json.loads(match.group(1))
            except ValueError:
                pending = None
                continue
            endpoints.append(pending)
            tokens.append("tok" if body.get("randomized_bundle_token") else "NO")
            pending = None
    print(f"SHAPE {path} n={len(endpoints)}")
    for index, (endpoint, token) in enumerate(zip(endpoints, tokens), start=1):
        print(f"SHAPE   {index}. {endpoint} {token}")
