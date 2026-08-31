"""TEMPORARY probe. Prints the bundle-token shape of each capture given.

Answers the positional question a whole-log count cannot: does the FIRST
captured request carry randomized_bundle_token? On a genuine first install
every request after the install response carries one, so only the first
request's state distinguishes a fresh install from a returning device.
"""
import json
import re
import sys

REQUEST_BODY = re.compile(r"Post value = (\{.*)$")

for path in sys.argv[1:]:
    shape = []
    for line in open(path, errors="replace"):
        match = REQUEST_BODY.search(line.rstrip())
        if not match:
            continue
        try:
            body = json.loads(match.group(1))
        except ValueError:
            continue
        shape.append("tok" if body.get("randomized_bundle_token") else "NO")
    first = shape[0] if shape else "none"
    print(f"SHAPE {path} n={len(shape)} first={first} all={','.join(shape)}")
