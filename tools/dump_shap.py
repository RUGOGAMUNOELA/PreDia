# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
si = c.find("_=()=>{if(!r)return")
send = c.find(
    'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:',
    si,
)
open("tools/shap_snip.txt", "w", encoding="utf-8").write(c[si:send])
print("wrote", send - si)
