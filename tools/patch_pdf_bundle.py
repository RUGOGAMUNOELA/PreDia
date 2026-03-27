# -*- coding: utf-8 -*-
"""Patch index-BnpSclTK.js: html2canvas + AndroidDownload PDF (risk + SHAP), capture roots, PNG multi-page."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app/src/main/assets/www/assets/index-BnpSclTK.js"
c = path.read_text(encoding="utf-8")

# Current minified handlers use .save() — does not use Android bridge in WebView.
v_old = (
    'v=()=>{const _=new xt;let x=20;_.setFontSize(16),_.text(`PreDia Risk Results for ${s}`,10,x),x+=10,_.setFontSize(12),_.text(`Risk score: ${u.toFixed(1)}%`,10,x),x+=7,_.text(`Risk level: ${c}`,10,x),x+=7,_.text(`P(Diabetes): ${(((r==null?void 0:r.probability_diabetes)??0)*100).toFixed(1)}%`,10,x),x+=7,_.text(`P(No diabetes): ${(((r==null?void 0:r.probability_no_diabetes)??0)*100).toFixed(1)}%`,10,x),_.save("t2dm_risk_results.pdf")}'
)

v_new = (
    'v=()=>{(async function(){try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!=="function"){try{var _hm=await import("./html2canvas.esm-QH1iLAAe.js");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure("PDF: html2canvas load "+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-risk-pdf-root");if(!root){try{AndroidDownload.exportJsFailure("PDF: missing capture root")}catch(z){}return}'
    'try{AndroidDownload.pdfBridgeLog("risk_pdf:h2c_start")}catch(z){}'
    'var canvas=await h2c(root,{scale:2,useCORS:true,allowTaint:true,backgroundColor:"#0b1120",logging:false,'
    'onclone:function(doc){try{doc.querySelectorAll(".no-print").forEach(function(n){n.style.setProperty("display","none","important")})}catch(e){}}});'
    'var imgData=canvas.toDataURL("image/png"),pdf=new xt({orientation:"p",unit:"mm",format:"a4"}),margin=8,'
    'pw=pdf.internal.pageSize.getWidth(),ph=pdf.internal.pageSize.getHeight(),imgW=pw-2*margin,'
    'imgH=canvas.height*imgW/canvas.width,pageInner=ph-2*margin,hLeft=imgH,pos=margin;'
    'pdf.addImage(imgData,"PNG",margin,pos,imgW,imgH);hLeft-=pageInner;'
    'while(hLeft>=0){pos=hLeft-imgH+margin;pdf.addPage();pdf.addImage(imgData,"PNG",margin,pos,imgW,imgH);hLeft-=pageInner}'
    'var d=pdf.output("datauristring"),m=d.indexOf(";base64,"),b64=m>=0?d.substring(m+8):(function(){var k=d.indexOf("base64,");return k>=0?d.substring(k+7):null})();'
    'if(!b64)throw new Error("no pdf b64");var fn="t2dm_risk_"+String(s||"patient").replace(/[^a-z0-9._-]+/gi,"_").replace(/^_+|_+$/g,"")+".pdf";'
    'AndroidDownload.beginPdfSave(fn);for(var ki=0;ki<b64.length;ki+=65536)AndroidDownload.appendPdfBase64Chunk(b64.substring(ki,ki+65536));AndroidDownload.finishPdfSave()'
    '}catch(e){try{AndroidDownload.exportJsFailure("risk_pdf:"+String(e&&e.message||e))}catch(z){}}})();}'
)

shap_old = (
    '_=()=>{if(!r)return;const x=new xt;let w=20;x.setFontSize(16),x.text(`SHAP explanation for ${c}`,10,w),w+=10,x.setFontSize(12);const P=b5(r.explanation_text||""),O=x.splitTextToSize(P||"Explanation not available.",180);x.text(O,10,w),x.save("t2dm_shap_explanation.pdf")}'
)

shap_new = (
    '_=()=>{(async function(){if(!r)return;try{var h2c=window.__preDiaHtml2Canvas;if(typeof h2c!=="function"){try{var _hm=await import("./html2canvas.esm-QH1iLAAe.js");window.__preDiaHtml2Canvas=h2c=_hm.default||_hm}catch(_e){try{AndroidDownload.exportJsFailure("PDF: html2canvas load "+String(_e&&_e.message||_e))}catch(z){}return}}if(!h2c){try{AndroidDownload.exportJsFailure("PDF: html2canvas not loaded")}catch(z){}return}'
    'var root=document.getElementById("preDia-shap-pdf-root");if(!root){try{AndroidDownload.exportJsFailure("PDF: missing SHAP root")}catch(z){}return}'
    'try{AndroidDownload.pdfBridgeLog("shap_pdf:h2c_start")}catch(z){}'
    'var canvas=await h2c(root,{scale:2,useCORS:true,allowTaint:true,backgroundColor:"#0b1120",logging:false,'
    'onclone:function(doc){try{doc.querySelectorAll(".no-print").forEach(function(n){n.style.setProperty("display","none","important")})}catch(e){}}});'
    'var imgData=canvas.toDataURL("image/png"),pdf=new xt({orientation:"p",unit:"mm",format:"a4"}),margin=8,'
    'pw=pdf.internal.pageSize.getWidth(),ph=pdf.internal.pageSize.getHeight(),imgW=pw-2*margin,'
    'imgH=canvas.height*imgW/canvas.width,pageInner=ph-2*margin,hLeft=imgH,pos=margin;'
    'pdf.addImage(imgData,"PNG",margin,pos,imgW,imgH);hLeft-=pageInner;'
    'while(hLeft>=0){pos=hLeft-imgH+margin;pdf.addPage();pdf.addImage(imgData,"PNG",margin,pos,imgW,imgH);hLeft-=pageInner}'
    'var d=pdf.output("datauristring"),m=d.indexOf(";base64,"),b64=m>=0?d.substring(m+8):(function(){var k=d.indexOf("base64,");return k>=0?d.substring(k+7):null})();'
    'if(!b64)throw new Error("no pdf b64");var fn="t2dm_shap_"+String(c||"patient").replace(/[^a-z0-9._-]+/gi,"_").replace(/^_+|_+$/g,"")+".pdf";'
    'AndroidDownload.beginPdfSave(fn);for(var ki=0;ki<b64.length;ki+=65536)AndroidDownload.appendPdfBase64Chunk(b64.substring(ki,ki+65536));AndroidDownload.finishPdfSave()'
    '}catch(e){try{AndroidDownload.exportJsFailure("shap_pdf:"+String(e&&e.message||e))}catch(z){}}})();}'
)

old_risk_root = 'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in",children:'
new_risk_root = 'return E.jsxs("div",{id:"preDia-risk-pdf-root",className:"max-w-5xl space-y-8 animate-fade-in",children:'

old_shap_root = 'return E.jsxs("div",{className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:'
new_shap_root = 'return E.jsxs("div",{id:"preDia-shap-pdf-root",className:"max-w-5xl space-y-8 animate-fade-in shap-explain-page min-w-0",children:'

if v_old not in c:
    raise SystemExit("v_old (risk .save handler) not found")
if shap_old not in c:
    raise SystemExit("shap_old not found")
if old_risk_root not in c:
    raise SystemExit("risk root JSX not found")
if c.count(old_shap_root) != 1:
    raise SystemExit(f"shap root count {c.count(old_shap_root)}")

c = c.replace(v_old, v_new, 1)
c = c.replace(shap_old, shap_new, 1)
c = c.replace(old_risk_root, new_risk_root, 1)
c = c.replace(old_shap_root, new_shap_root, 1)

path.write_text(c, encoding="utf-8")
print("OK:", path)
