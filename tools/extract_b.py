# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
i = c.find(",b=()=>{const k=")
j = c.find("},v=()=>{const _=new xt", i)
open("tools/b_block.txt", "w", encoding="utf-8").write(c[i:j])
print(len(c[i:j]))
