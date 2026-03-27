# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
idx = c.find("Patient name")
print("idx", idx)
open("tools/patient_chunk.txt", "w", encoding="utf-8").write(c[idx - 800 : idx + 3500])
