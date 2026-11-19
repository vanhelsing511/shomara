#!/usr/bin/env python3
"""Generate R.kt (Kotlin constants) from `aapt2 dump resources` output."""
import re
import sys


def main():
    src, dst = sys.argv[1], sys.argv[2]
    groups = {}
    for line in open(src, encoding="utf-8", errors="ignore"):
        m = re.search(r"resource 0x([0-9a-f]{8}) ([a-z0-9_]+)/([A-Za-z0-9_]+)", line)
        if m:
            rid, rtype, name = m.group(1), m.group(2), m.group(3)
            groups.setdefault(rtype, {})[name] = "0x" + rid
    out = ["package ir.shomara.app", "", "object R {"]
    for rtype in sorted(groups):
        obj = rtype.replace("-", "_")
        if obj[0].isdigit():
            obj = "R" + obj
        out.append("    object %s {" % obj)
        for name in sorted(groups[rtype]):
            out.append("        const val %s = %s" % (name, groups[rtype][name]))
        out.append("    }")
    out.append("}")
    open(dst, "w", encoding="utf-8").write("\n".join(out) + "\n")
    print("R.kt: " + str(sum(len(v) for v in groups.values())) + " ids, types: " + ",".join(sorted(groups)))


if __name__ == "__main__":
    main()
