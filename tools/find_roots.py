# -*- coding: utf-8 -*-
import re
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
needle = 'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in",children:'
for m in re.finditer(re.escape(needle), c):
    start = max(0, m.start() - 120)
    print("--- at", m.start(), "---")
    print(c[start : m.start() + 80])
    print()
