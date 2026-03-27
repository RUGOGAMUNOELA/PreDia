# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
needles = [
    ",b=()=>{const k=",
    "v=()=>{const _=new xt",
    "_=()=>{if(!r)return",
    "shap-explain-page",
    'max-w-5xl space-y-8 animate-fade-in",children',
    "Export CSV",
    "onClick:b,",
]
for s in needles:
    i = c.find(s)
    print(repr(s), "->", i)
