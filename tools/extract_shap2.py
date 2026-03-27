# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
si = c.find("_=()=>{if(!r)return")
needle = '})()};return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0"'
idx = c.find(needle, si)
assert idx != -1, "needle not found"
end = idx + len("})()};")
snip = c[si:end]
open("tools/shap_snip.txt", "w", encoding="utf-8").write(snip)
print("len", len(snip))
print(snip[:120])
print("...")
print(snip[-120:])
