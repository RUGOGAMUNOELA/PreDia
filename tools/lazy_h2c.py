# -*- coding: utf-8 -*-
path = "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = open(path, encoding="utf-8").read()

risk_old = (
    'v=()=>{(async function(){try{var h2c=window.__preDiaHtml2Canvas;if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-risk-pdf-root");'
)
risk_new = (
    'v=()=>{(async function(){try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!=="function"){try{var _hm=await import("./html2canvas.esm-QH1iLAAe.js");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure("PDF: html2canvas load "+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-risk-pdf-root");'
)

shap_old = (
    '_=()=>{(async function(){if(!r)return;try{var h2c=window.__preDiaHtml2Canvas;if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-shap-pdf-root");'
)
shap_new = (
    '_=()=>{(async function(){if(!r)return;try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!=="function"){try{var _hm=await import("./html2canvas.esm-QH1iLAAe.js");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure("PDF: html2canvas load "+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-shap-pdf-root");'
)

if risk_old not in c:
    raise SystemExit("risk_old not found")
if shap_old not in c:
    raise SystemExit("shap_old not found")
c = c.replace(risk_old, risk_new, 1)
c = c.replace(shap_old, shap_new, 1)
open(path, "w", encoding="utf-8").write(c)
print("patched lazy h2c")
