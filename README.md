# PreDia — T2DM Risk Prediction Tool

**PreDia** is an Android app for **Type 2 Diabetes Mellitus (T2DM) risk prediction** in the field. It is built for the **Village Health Teams in Uganda** and supports screening and education in low resource settings.

# Group name: The Ravens
Rugogamu Noela S23B38/016 B22775
Arinaitwe Paula Lorraine
Ssendi Malon Aloysious
---

## What it does

- **Risk prediction**  
  Users enter patient and lifestyle data (e.g. glucose, BMI, age, diet, physical activity). The app uses a **hybrid engine of Machine Learning and SHAP** to estimate T2DM risk and show results on a **Risk Dashboard**.

- **SHAP Explain**  
  Explains how each input (e.g. glucose, BMI) contributes to the risk score, so health workers can interpret and communicate results.

- **Data & export**  
  - View and manage screening records.  
  - **Export data as CSV** and **save reports as PDF** (e.g. Risk Dashboard, SHAP page).  
  - Files are saved via the system “Save to” dialog (e.g. Downloads folder).

- **Offline-friendly**  
  Core screening and prediction can work with data stored on the device when offline.

- **Bilingual**  
  Supports English and Luganda for key labels and messages.

---

## Who it’s for

- Health workers and screeners in the field  
- Ministry of Health and partners for T2DM screening and education  

**Disclaimer:** For screening and education only. Not a substitute for professional medical advice.

---

## Tech overview

- **Android app** (Kotlin): single `WebView` that loads the in-app web UI from `assets/www/`.
- **In-app UI**: built as a web app (e.g. React/Vite), then bundled and placed under `app/src/main/assets/www/` (HTML, JS, CSS, favicon).
- **Features**: login, input form, Risk Dashboard, SHAP Explain, Data/Export, Models. PDF and CSV are generated in the web layer; the app intercepts download actions and uses the Android “Save to” dialog so users can save files (e.g. to Downloads).

---

## How to build and run

1. Open the project in **Android Studio**.  
2. Use a device or emulator with **API 21+**.  
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)** (or Run).  
4. Install the APK and open **PreDia**; sign in and use the Input Form, Risk Dashboard, SHAP Explain, and Export/Data as needed.

---

## Repository structure (high level)

- `app/` — Android app (Kotlin, `MainActivity`, theme, manifest).  
- `app/src/main/assets/www/` — In-app web UI (HTML, JS, CSS, favicon).  
- `app/src/main/res/` — Android resources (icons, themes, strings).

---

## Licence and attribution

Developed for the **Republic of Uganda · Ministry of Health**.  
Hybrid ML + SHAP T2DM risk prediction for use in screening and education.
