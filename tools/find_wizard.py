# -*- coding: utf-8 -*-
p = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(p, encoding="utf-8").read()
# find Patient name label in UI
for label in ['t("Patient', "patient_name", "Patient name", "Enter patient"]:
    print(label, c.find(label))

# search for Continue near patient_name string in source
idx = c.find("patient_name")
count = 0
while idx != -1 and count < 15:
    chunk = c[max(0, idx - 300) : idx + 500]
    if "Continue" in chunk and "onClick" in chunk:
        print("\n=== match at", idx, "===")
        print(chunk)
        print()
    idx = c.find("patient_name", idx + 1)
    count += 1
