/**
 * Report PDFs for Risk and SHAP pages: chart capture (html2canvas) + native Android PdfDocument.
 */
import * as h2cMod from "./html2canvas.esm-QH1iLAAe.js";

const html2canvas = h2cMod.default || h2cMod;

function ensureHtml2Canvas() {
  if (typeof window !== "undefined" && typeof window.__preDiaHtml2Canvas === "function") {
    return window.__preDiaHtml2Canvas;
  }
  return html2canvas;
}

function safeFileNameSegment(name) {
  return String(name || "patient")
    .replace(/[^a-z0-9._-]+/gi, "_")
    .replace(/^_+|_+$/g, "");
}

/**
 * Streams chart PNG (base64) + JSON metadata to Kotlin, which builds a valid PDF with PdfDocument.
 */
function streamNativePdfToAndroid(fileNamePrefix, chartPngDataUrl, metaObj) {
  const fn = fileNamePrefix + "_" + safeFileNameSegment(metaObj.patientName) + ".pdf";
  AndroidDownload.beginNativePdfSave(fn);
  const chartPart =
    chartPngDataUrl && chartPngDataUrl.indexOf(",") >= 0
      ? chartPngDataUrl.split(",")[1]
      : chartPngDataUrl || "";
  const step = 8192;
  for (let i = 0; i < chartPart.length; i += step) {
    AndroidDownload.appendNativePdfChartChunk(chartPart.substring(i, i + step));
  }
  AndroidDownload.setNativePdfMetadata(JSON.stringify(metaObj));
  AndroidDownload.finishNativePdfSave();
}

async function captureChartPngDataUrl(selector) {
  const el = document.querySelector(selector);
  if (!el) {
    return null;
  }
  const h2c = ensureHtml2Canvas();
  try {
    if (typeof AndroidDownload !== "undefined" && AndroidDownload.pdfBridgeLog) {
      AndroidDownload.pdfBridgeLog("pdf_report:chart_h2c");
    }
  } catch (e) {}
  const canvas = await h2c(el, {
    scale: 2,
    useCORS: true,
    allowTaint: true,
    backgroundColor: "#ffffff",
    logging: false,
  });
  return canvas.toDataURL("image/png");
}

export async function exportRiskReportPdf(opts) {
  const {
    patientName,
    riskScore,
    riskLevelLabel,
    pDiabetesPct,
    pNoDiabetesPct,
    explanationText,
    chartSelector = "#preDia-risk-chart-capture",
  } = opts;

  let chartPngDataUrl = null;
  try {
    chartPngDataUrl = await captureChartPngDataUrl(chartSelector);
  } catch (e) {
    try {
      if (typeof AndroidDownload !== "undefined" && AndroidDownload.exportJsFailure) {
        AndroidDownload.exportJsFailure("risk chart:" + e);
      }
    } catch (z) {}
  }

  const summaryBlock =
    "Risk Score: " +
    Number(riskScore).toFixed(1) +
    "%\n" +
    "Risk Level: " +
    String(riskLevelLabel || "") +
    "\n" +
    "P(Diabetes): " +
    String(pDiabetesPct) +
    "\n" +
    "P(No Diabetes): " +
    String(pNoDiabetesPct);

  streamNativePdfToAndroid(
    "t2dm_risk",
    chartPngDataUrl,
    {
      kind: "risk",
      patientName: String(patientName || "Patient"),
      summaryBlock,
      explanationText: String(explanationText || ""),
    }
  );
}

export async function exportShapReportPdf(opts) {
  const {
    patientName,
    introductionText,
    recommendationsText: recRaw,
    chartSelector = "#preDia-shap-chart-capture",
  } = opts;
  const recommendationsText =
    String(recRaw || "").trim() ||
    "No itemized recommendations were available for this result.";
  const introBody =
    String(introductionText || "").trim() ||
    "No summary text was returned for this explanation.";

  let chartPngDataUrl = null;
  try {
    chartPngDataUrl = await captureChartPngDataUrl(chartSelector);
  } catch (e) {
    try {
      if (typeof AndroidDownload !== "undefined" && AndroidDownload.exportJsFailure) {
        AndroidDownload.exportJsFailure("shap chart:" + e);
      }
    } catch (z) {}
  }

  streamNativePdfToAndroid(
    "t2dm_shap",
    chartPngDataUrl,
    {
      kind: "shap",
      patientName: String(patientName || "Patient"),
      introBody,
      recommendationsText,
    }
  );
}
