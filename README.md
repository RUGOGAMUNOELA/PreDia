# PreDia — Offline Bilingual T2DM Clinical Risk Prediction (Android APK)

**Group name:** The Ravens  

**Final-year project (2025–2026)** — Type 2 Diabetes Mellitus (T2DM) screening and education for rural Uganda, aligned with Ministry of Health priorities.

---

## Group members

| Name | Registration | Email |
|------|--------------|--------|
| Rugogamu Noela | S23B38/016 · B22775 | rugogamunoela@gmail.com |
| Arinaitwe Lorraine Paula | M23B38/004 | lorrainearinaitwe@gmail.com |
| Ssendi Malon Aloysious | S23B38/002 · B21258 | ssendimalon2013@gmail.com |

---

## Related repositories (project line of sight)

The same clinical and machine-learning vision is expressed in three stages on GitHub:

| Stage | Repository | Role |
|--------|------------|------|
| **1. Early prototype** | [DS-Research-The-Ravens](https://github.com/LorraineA8/DS-Research-The-Ravens) | Notebooks, data, Streamlit CDSS — initial diabetes risk pipeline, model artefacts, and demonstration UI. |
| **2. Full-stack web application** | [T2DM_Risk_Prediction_Tool](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool) | FastAPI backend, React (Vite + Tailwind) frontend, hybrid ML + SHAP, optional MySQL, PWA-oriented docs, bilingual (English / Luganda) integration. |
| **3. Final deployable client (this product)** | [PreDia](https://github.com/RUGOGAMUNOELA/PreDia) | **Android APK**: WebView-hosted UI bundled as assets, Kotlin bridges for storage and **native PDF generation**, optimised for installation on low-end devices in the field. |

**This README describes repository (3)** — the installable Android application. For training scripts, API contracts, and extended documentation (`docs/PROJECT_REPORT.md`, deployment, PWA), see the [web app repository](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool).

---

## Introduction

**PreDia** is an Android application that packages the project’s risk-prediction and explainability experience into a single installable file. Health workers open the app, sign in, enter patient and clinical data, receive a **hybrid** risk estimate (machine learning plus WHO-style clinical rules), review **SHAP-based** explanations, and save **CSV** and **PDF** outputs through Android’s standard storage flows.

The tool targets **Village Health Teams (VHTs)**, peripheral clinics, and programme staff who need **fast**, **understandable**, and **documented** screening when advanced laboratory infrastructure is limited or absent.

---

## Problem statement

The following is the **exact** problem statement used in the [T2DM_Risk_Prediction_Tool](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool) web application README (statistics and wording preserved):

> *"Over 369,000 adults live with Diabetes in Uganda. That is (1 in every 14 people), 225,900 of these cases are undiagnosed. 160,000 of these come from villages with a threatening potential of the total diabetes number rising to 1.2 million by 2050. Basing on the fact that more than 37,500,000 people of Uganda’s population lives in rural areas. Village Health Teams have no easy way to find people at risk early therefore when diabetes is found late, people already have eye damage, kidney failure of foot ulcers. This is a big problem that we can solve with a T2DM Clinical Risk Predictive Tool."*

---

## Objectives (project deliverables)

The project defines **three objectives**, aligned with the three staged **deliverables** (same wording as the web app README):

1. **Deliverable 1 (Feb 11):** Trained/evaluated ML model with Ugandan augmentation and selected-feature pipeline; **TRL 4**.  
2. **Deliverable 2 (Feb 28):** Hybrid system with XAI, bias mitigation, web UI (gender first input, pregnancy for Female only), optional MySQL and CSV export; **TRL 5**.  
3. **Deliverable 3 (March 11):** Complete and fully functioning risk prediction tool: web application with a responsive design, offline functionality, and a bilingual interface (Luganda and English); **TRL 6** — including the **PreDia Android APK** as the bundled field client in this repository.

---

## Methodology

The methodology below follows the **RAVENS project poster** (methodology section): end-to-end steps from data to offline APK.

- **Data preparation:** Collected a T2DM dataset with easy-to-measure demographic, clinical, and lifestyle variables, then cleaned and encoded it for modelling.  
- **Model training:** Trained and tuned an **XGBoost** classifier on this dataset, evaluating performance using validation metrics such as **AUC**, **accuracy**, **sensitivity**, and **specificity**.  
- **Explainability:** Applied **SHAP** to the final model to obtain global feature importance and per-patient explanations of which factors drive predicted risk.  
- **Sunbird integration:** Implemented a **Sunbird API** layer so that predictions and screening events can be formatted and sent in a standardised way, enabling future interoperability with digital health systems.  
- **App implementation:** Built a **React/Vite** web app (Input Form, Risk Dashboard, SHAP Explain, Data/Export) and wired it to the XGBoost + SHAP + Sunbird logic.  
- **Offline Android deployment:** Embedded the web app in an **Android WebView**, added a **Kotlin** bridge for PDF/CSV export, and packaged everything into the **PreDia APK** for offline use on low-cost devices.

---

## Model evaluation and metrics

The deployed **XGBoost** classifier (as reported in project evaluation materials) achieved strong discrimination on the held-out test set, for example:

| Metric | Value (reported test set) |
|--------|---------------------------|
| **ROC AUC** | 0.9672 |
| **Accuracy** | 0.8896 |
| **Precision** | 0.8246 |
| **Recall** | 0.8704 |
| **F1 score** | 0.8468 |

*Exact train/test splits and cohort definitions are documented in the notebooks and in [T2DM_Risk_Prediction_Tool](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool) (`docs/`, notebooks).*

**Features** used in the screening pipeline include routinely collectable variables such as: Pregnancies (where applicable), Glucose, Blood Pressure, BMI, Diabetes Pedigree Function, Age, Physical Activity, Diet Quality, Alcohol Use, Smoking — chosen so that prediction remains feasible in **low-resource** settings.

---

## Technology stack — PreDia (this repository)

| Component | Technology |
|-----------|------------|
| **Mobile shell** | Kotlin, Android SDK **minSdk 24**, `ComponentActivity` |
| **UI delivery** | Single **WebView** loading bundled assets via **WebViewAssetLoader** (`https://appassets.androidplatform.net/...` → `assets/www/`) so ES modules load reliably |
| **Embedded front end** | Production build of **React + Vite + Tailwind** (static HTML, JS, CSS under `app/src/main/assets/www/`) |
| **Bridge** | `JavascriptInterface` (**`WebExportBridge`**) for data-URL / blob handling and native PDF handoff |
| **PDF generation** | **Native** `android.graphics.pdf.PdfDocument` (**`NativePdfReportGenerator`**) — white pages, text layout, chart raster from **html2canvas** in the web bundle; avoids corrupt full-PDF base64 transfer from JavaScript |
| **Charts / export hooks** | Bundled **html2canvas** (chart capture for PDF); system **Storage Access Framework** for user-chosen save location |

---

## Project evolution: prototype → web app → PreDia APK

### Stage A — Prototype ([DS-Research-The-Ravens](https://github.com/LorraineA8/DS-Research-The-Ravens))

Early work concentrated on **notebooks**, **cleaned data**, **model training**, **SHAP**, and a **Streamlit** client to prove predictions and explanations end-to-end.

### Stage B — Full-stack web application ([T2DM_Risk_Prediction_Tool](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool))

The system matured into:

- **FastAPI** services: authentication, prediction, SHAP, exports, translation hooks.  
- **React** SPA: login, input form, risk dashboard, SHAP explain, data/models pages, export screens.  
- **Optional MySQL** for accounts and screening records; **CSV** export by date range; **PWA** documentation for low-connectivity use.  
- **Sunbird AI** (or equivalent integration as configured) for **Luganda / English** support on key strings.

### Stage C — PreDia APK ([PreDia](https://github.com/RUGOGAMUNOELA/PreDia) — **this repo**)

The **same UI** is **compiled to static files** and **shipped inside the APK**. Android provides:

- Installation and updates through standard app channels.  
- **Native PDF** creation so reports are **valid binary PDFs** on device.  
- **Scoped storage** and the **Save to** dialog for CSV/PDF without relying on desktop browser download behaviour.

---

## How the final product works (architecture of the APK)

1. **Launch:** The user opens PreDia; `MainActivity` hosts one **WebView**.  
2. **Asset serving:** Requests to the mapped **HTTPS** origin are served from **`app/src/main/assets/www/`**, avoiding broken module loading on `file://`.  
3. **Authentication and navigation:** The embedded SPA performs login and routing (Input Form, Risk, SHAP, Data, Models, Export) as in the web build.  
4. **Prediction and SHAP:** Logic runs in the bundled JavaScript runtime; model and SHAP artefacts are those packaged for the client build.  
5. **Exports:**  
   - **CSV / generic binary:** Data URLs and streams are passed to Kotlin; the app opens **`CreateDocument`** / output streams so the user picks a folder or filename.  
   - **PDF:** The web layer sends **chart image (PNG)** plus **structured text metadata** over the bridge; **`NativePdfReportGenerator`** builds a **PdfDocument**, finishes pages, writes bytes, closes the document — producing **openable** PDF files on Android and when copied elsewhere.  
6. **Offline use:** Once installed, core flows operate **without a network** connection for bundled functionality; any server-dependent features follow the configuration embedded in that build.

---

## What PreDia does for end users

| User need | PreDia behaviour |
|-----------|------------------|
| **Rapid screening** | Single app; guided input; immediate risk score and level (e.g. Low / Moderate / High). |
| **Trust and counselling** | SHAP contributions and plain-language sections explain **why** the model ranked risk as it did. |
| **Clinical proportionality** | Hybrid rules escalate risk when raw clinical values demand it, independent of probability alone. |
| **Language** | English and Luganda where strings are wired in the bundled UI. |
| **Documentation** | PDF reports for risk and SHAP views; CSV export where implemented in the bundle for programme data. |
| **Field hardware** | Targets **low-end Android** devices common among community health programmes. |

**Disclaimer:** PreDia supports **screening and education** only. It does **not** replace professional medical diagnosis or treatment.

---

## Business Model Canvas

**Source:** This section is summarised from the team’s **Business Model Canvas** written submission (the dedicated BMC document). It is **not** taken from the project poster’s methodology block.

The canvas describes how the product connects **segments**, **value**, **revenue**, and **costs**.

### Customer segments

- **Village Health Teams and community health workers** — community screening, limited equipment, need speed and clarity.  
- **Health facilities** — nurses, clinical officers, physicians who require **standardised** workflows, dashboards, and auditable exports.  
- **Programme managers and MOH / NGO partners** — oversight of NCD programmes; need aggregate-ready data and transparent methodology.  
- **Screened adults** — indirect beneficiaries of clearer feedback and earlier referral conversations.

### Value proposition

- Fast T2DM **risk stratification** using **simple inputs**.  
- **Explainable** outputs (SHAP) and **hybrid clinical rules** for safety and credibility.  
- **Bilingual** access and **offline-capable** client (APK) for rural connectivity.  
- **Exports** for accountability and research (CSV/PDF).

### Revenue streams (strategic options)

Examples analysed in the canvas include **licence or subscription** fees for institutions, **grant- or programme-funded** deployments, **training and support** packages, and optional **analytics** services. These are **planning constructs** for sustainability discussions, not statements of current commercial terms.

### Cost structure (strategic overview)

Typical cost buckets include **cloud or server infrastructure** (for any online backend), **software maintenance and model retraining**, **data and translation services**, **field training and travel**, **partnerships and compliance**, and **contingency**. The student implementation prioritises **open-source tooling** and **phased rollout** to control expense.

---

## Repository layout (PreDia)

```
PreDia/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/t2dm/predia/
│       │   ├── MainActivity.kt              # WebView, WebViewAssetLoader, export flows
│       │   └── NativePdfReportGenerator.kt  # PdfDocument lifecycle, text + chart layout
│       ├── assets/www/                      # Bundled React/Vite build (index.html, JS, CSS, images)
│       └── res/                             # Themes, mipmap, values
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── .gitignore
└── README.md
```

---

## How to build and run

1. Install **Android Studio** (current stable channel).  
2. Open the **PreDia** project folder.  
3. Select a run configuration targeting **API 24+** (emulator or physical device).  
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)** or click **Run**.  
5. Install the generated APK, launch **PreDia**, complete login, then exercise **Input Form**, **Risk Dashboard**, **SHAP Explain**, and **Export** (CSV/PDF save dialogs).

For **backend**, **notebooks**, and **environment variables** for the full web stack, clone [T2DM_Risk_Prediction_Tool](https://github.com/RUGOGAMUNOELA/T2DM_Risk_Prediction_Tool) and follow `docs/RUN_INSTRUCTIONS.md` and `docs/RUN_REACT_APP.md`.

---

## Licence, attribution, and disclaimer

- **Attribution:** Developed in support of **T2DM screening and health education** aligned with **Republic of Uganda · Ministry of Health** priorities.  
- **Licence:** Academic and educational use unless otherwise specified in a future `LICENSE` file in this repository.  
- **Medical disclaimer:** PreDia is **not** a substitute for professional medical advice, diagnosis, or treatment. Clinical decisions rest with qualified providers. Model outputs are **screening aids**, not definitive diagnoses.

---

**Repository:** [https://github.com/RUGOGAMUNOELA/PreDia](https://github.com/RUGOGAMUNOELA/PreDia)
