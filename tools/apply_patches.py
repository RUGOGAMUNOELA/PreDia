# -*- coding: utf-8 -*-
"""Apply PreDia www bundle patches (PDF capture, CSV button, validation)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = path.read_text(encoding="utf-8")

b_remove = Path(__file__).parent.joinpath("b_block.txt").read_text(encoding="utf-8") + "}"
if b_remove not in c:
    raise SystemExit("b_remove block not found")
c = c.replace(b_remove, "", 1)

v_old = Path(__file__).parent.joinpath("v_snip.txt").read_text(encoding="utf-8")
PX = "25.4/96"
v_new = (
    "v=()=>{(async function(){try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!==\"function\"){try{var _hm=await import(\"./html2canvas.esm-QH1iLAAe.js\");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure(\"PDF: html2canvas load \"+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure(\"PDF: html2canvas not loaded\")}catch(z){}return}"
    'var root=document.getElementById("preDia-risk-pdf-root");if(!root){try{AndroidDownload.exportJsFailure("PDF: missing capture root")}catch(z){}return}'
    'try{AndroidDownload.pdfBridgeLog("risk_pdf:h2c_start")}catch(z){}'
    'var canvas=await h2c(root,{scale:2,useCORS:true,allowTaint:true,backgroundColor:"#0b1120",logging:false,'
    'onclone:function(doc){try{doc.querySelectorAll(".no-print").forEach(function(n){n.style.setProperty("display","none","important")})}catch(e){}}});'
    'var imgData=canvas.toDataURL("image/jpeg",0.88),pdf=new xt({orientation:"p",unit:"mm",format:"a4"}),'
    'pageW=pdf.internal.pageSize.getWidth(),pageH=pdf.internal.pageSize.getHeight(),margin=8,innerW=pageW-2*margin,innerH=pageH-2*margin,'
    f"mmW=canvas.width*({PX}),mmH=canvas.height*({PX}),sc=Math.min(innerW/mmW,innerH/mmH),dw=mmW*sc,dh=mmH*sc,"
    'ox=margin+(innerW-dw)/2,oy=margin+(innerH-dh)/2;'
    'pdf.addImage(imgData,"JPEG",ox,oy,dw,dh,void 0,"FAST");'
    'var d=pdf.output("datauristring"),m=d.indexOf(";base64,"),b64=m>=0?d.substring(m+8):(function(){var k=d.indexOf("base64,");return k>=0?d.substring(k+7):null})();'
    'if(!b64)throw new Error("no pdf b64");AndroidDownload.beginPdfSave("t2dm_risk_results.pdf");'
    'for(var ki=0;ki<b64.length;ki+=65536)AndroidDownload.appendPdfBase64Chunk(b64.substring(ki,ki+65536));AndroidDownload.finishPdfSave()'
    '}catch(e){try{AndroidDownload.exportJsFailure("risk_pdf:"+String(e&&e.message||e))}catch(z){}}})();'
)
if v_old not in c:
    raise SystemExit("v_old not found")
c = c.replace(v_old, v_new, 1)

old_risk_root = 'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in",children:'
new_risk_root = 'return E.jsxs("div",{id:"preDia-risk-pdf-root",className:"max-w-5xl space-y-8 animate-fade-in",children:'
idx_rr = c.find(old_risk_root)
if idx_rr == -1:
    raise SystemExit("risk root not found")
c = c[:idx_rr] + new_risk_root + c[idx_rr + len(old_risk_root) :]

old_btn = 'E.jsxs("div",{className:"flex gap-2 no-print flex-wrap",children:[E.jsx("button",{type:"button",onClick:b,className:"btn-secondary inline-flex items-center gap-2",children:"Export CSV"}),E.jsxs("button",{type:"button",onClick:v,className:"btn-secondary inline-flex items-center gap-2",children:[E.jsx(CO,{}),"Print / Save PDF"]})]})'
new_btn = 'E.jsx("div",{className:"flex gap-2 no-print flex-wrap",children:E.jsxs("button",{type:"button",onClick:v,className:"btn-secondary inline-flex items-center gap-2",children:[E.jsx(CO,{}),"Print / Save PDF"]})})'
if old_btn not in c:
    raise SystemExit("export csv button not found")
c = c.replace(old_btn, new_btn, 1)

old_shap_root = 'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:'
new_shap_root = 'return E.jsxs("div",{id:"preDia-shap-pdf-root",className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:'
if c.count(old_shap_root) != 1:
    raise SystemExit(f"shap root count {c.count(old_shap_root)}")
c = c.replace(old_shap_root, new_shap_root, 1)

shap_old = Path(__file__).parent.joinpath("shap_snip.txt").read_text(encoding="utf-8")
shap_new = (
    "_=()=>{(async function(){if(!r)return;try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!==\"function\"){try{var _hm=await import(\"./html2canvas.esm-QH1iLAAe.js\");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure(\"PDF: html2canvas load \"+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure(\"PDF: html2canvas not loaded\")}catch(z){}return}"
    'var root=document.getElementById("preDia-shap-pdf-root");if(!root){try{AndroidDownload.exportJsFailure("PDF: missing SHAP root")}catch(z){}return}'
    'try{AndroidDownload.pdfBridgeLog("shap_pdf:h2c_start")}catch(z){}'
    'var canvas=await h2c(root,{scale:2,useCORS:true,allowTaint:true,backgroundColor:"#0b1120",logging:false,'
    'onclone:function(doc){try{doc.querySelectorAll(".no-print").forEach(function(n){n.style.setProperty("display","none","important")})}catch(e){}}});'
    'var imgData=canvas.toDataURL("image/jpeg",0.88),pdf=new xt({orientation:"p",unit:"mm",format:"a4"}),'
    'pageW=pdf.internal.pageSize.getWidth(),pageH=pdf.internal.pageSize.getHeight(),margin=8,innerW=pageW-2*margin,innerH=pageH-2*margin,'
    f"mmW=canvas.width*({PX}),mmH=canvas.height*({PX}),sc=Math.min(innerW/mmW,innerH/mmH),dw=mmW*sc,dh=mmH*sc,"
    'ox=margin+(innerW-dw)/2,oy=margin+(innerH-dh)/2;'
    'pdf.addImage(imgData,"JPEG",ox,oy,dw,dh,void 0,"FAST");'
    'var d=pdf.output("datauristring"),m=d.indexOf(";base64,"),b64=m>=0?d.substring(m+8):(function(){var k=d.indexOf("base64,");return k>=0?d.substring(k+7):null})();'
    'if(!b64)throw new Error("no pdf b64");AndroidDownload.beginPdfSave("t2dm_shap_explanation.pdf");'
    'for(var ki=0;ki<b64.length;ki+=65536)AndroidDownload.appendPdfBase64Chunk(b64.substring(ki,ki+65536));AndroidDownload.finishPdfSave()'
    '}catch(e){try{AndroidDownload.exportJsFailure("shap_pdf:"+String(e&&e.message||e))}catch(z){}}})();'
)
if shap_old not in c:
    raise SystemExit("shap_old not found")
c = c.replace(shap_old, shap_new, 1)

ql_old = "async function QL(e){const t={Pregnancies:e.pregnancies"
ql_new = 'async function QL(e){const __pn=(e.patient_name||"").trim();if(!__pn||!/[A-Za-z\\u00C0-\\u024F]/.test(__pn))throw new Error("Enter a valid patient name using letters (not only numbers or symbols).");const t={Pregnancies:e.pregnancies'
if ql_old not in c:
    raise SystemExit("QL not found")
c = c.replace(ql_old, ql_new, 1)

wiz_old = 'onClick:()=>a("form"),className:"btn-primary mt-6",children:n("Continue")'
wiz_new = 'onClick:()=>{const _pn=(s||"").trim();if(!_pn||!/[A-Za-z\\u00C0-\\u024F]/.test(_pn)){alert("Please enter a patient name using letters (not only numbers or symbols).");return}a("form")},className:"btn-primary mt-6",children:n("Continue")'
if wiz_old not in c:
    raise SystemExit("wizard continue not found")
c = c.replace(wiz_old, wiz_new, 1)

path.write_text(c, encoding="utf-8")
print("OK:", path)
