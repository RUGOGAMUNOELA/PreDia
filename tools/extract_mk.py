import re
p = r"app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
i = c.find("function MK(")
print(c[i : i + 12000])
