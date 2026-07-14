# Fake Product Review Detector

A Java version of the original Python Streamlit demo. The app trains a small TF-IDF-style logistic classifier from `src/main/resources/reviews.csv`, serves a browser UI with Java's built-in HTTP server, and explains suspicious patterns such as promotional keywords, repeated exclamation marks, repeated words, uppercase text, and very short reviews.

## Features

- Single review fake/genuine detection
- Confidence score and fake-probability score
- Risk level: Low, Medium, or High
- Suspicious keyword matching
- Word, character, and exclamation-mark counts
- Batch analysis with one review per line
- Training dataset dashboard stats
- Browser UI and simple HTTP endpoints

## Requirements

- JDK 17 or newer

## Run

```powershell
.\build.ps1
.\run.ps1
```

Open http://localhost:8080 in your browser.

If you are using Command Prompt instead of PowerShell:

```cmd
powershell -ExecutionPolicy Bypass -File .\build.ps1
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

## API Endpoints

### Analyze One Review

```http
POST /predict
Content-Type: application/x-www-form-urlencoded

review=Best product ever!!! Must buy!!!
```

### Analyze Multiple Reviews

```http
POST /batch
Content-Type: application/x-www-form-urlencoded

reviews=Very good quality and worth the price
Best product ever!!! Must buy!!!
```

### Dataset Stats

```http
GET /stats
```

## Project Structure

```text
src/main/java/com/fakereviewdetector/
  DashboardStats.java
  FakeReviewDetectorApp.java
  FakeReviewClassifier.java
  CsvReviewLoader.java
  PredictionResult.java
  ReviewSample.java

src/main/resources/
  index.html
  reviews.csv
```

The original Python files are left in place for reference. The Java app does not depend on `venv`, `requirements.txt`, or `model.pkl`.
