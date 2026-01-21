# 🕵️ Fake Product Review Detector (Mini AI)

A Machine Learning web app that detects whether an e-commerce product review is **Fake** or **Genuine** using NLP.

Demo link:https://charanvitha-fake-product-review-detector-app-3i8p0p.streamlit.app/

## 🚀 Features
- Detects Fake vs Genuine reviews
- Shows prediction confidence score
- Displays suspicious patterns (keywords, punctuation, short reviews)
- Simple Streamlit UI

## 🛠 Tech Stack
- Python
- Scikit-learn
- TF-IDF Vectorizer
- Logistic Regression
- Streamlit

## 📂 Project Structure
fake-review-detector/
│── reviews.csv
│── train_model.py
│── app.py
│── model.pkl
│── requirements.txt

## ▶️ How to Run
```bash
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python train_model.py
streamlit run app.py
