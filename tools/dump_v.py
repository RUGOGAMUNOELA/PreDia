# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
k = c.find("v=()=>{const _=new xt")
end = c.find('return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in",children:', k)
open("tools/v_snip.txt", "w", encoding="utf-8").write(c[k:end])
print("wrote", end - k, "chars")
