# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
# b block
i = c.find(",b=()=>{const k=")
j = c.find("},v=()=>{const _=new xt", i)
print("b len", j - i)
print(c[i : i + 200])
print("...")
print(c[j - 80 : j + 40])
# v block end - find return E.jsxs with max-w-5xl
k = c.find("v=()=>{const _=new xt")
end = c.find('return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in",children:', k)
print("v block len", end - k)
print(c[k : k + 150])
print("...")
print(c[end - 100 : end + 80])
# shap _
si = c.find("_=()=>{if(!r)return")
send = c.find('return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:', si)
print("shap len", send - si)
print(c[si : si + 200])
print("...")
print(c[send - 80 : send + 100])
