import streamlit as st
import joblib

# Load trained model
model = joblib.load("model.pkl")

st.set_page_config(page_title="Fake Review Detector", page_icon="🕵️")

st.title("🕵️ Fake Product Review Detector (Mini AI)")
st.write("Paste a review and detect whether it is **Fake** or **Genuine**.")

review = st.text_area("✍️ Enter Review Text:", height=160)

if st.button("Detect Review"):
    if review.strip() == "":
        st.warning("⚠️ Please enter a review first.")
    else:
        pred = model.predict([review])[0]
        prob = model.predict_proba([review])[0]
        confidence = prob[pred] * 100

        if pred == 1:
            st.error("❌ Fake Review Detected")
        else:
            st.success("✅ Genuine Review Detected")

        st.info(f"📌 Confidence: {confidence:.2f}%")

        # Explainability (unique feature)
        suspicious_keywords = ["best", "must buy", "amazing", "superb", "perfect", "unbelievable", "buy now"]
        reasons = []

        for word in suspicious_keywords:
            if word in review.lower():
                reasons.append(f"Contains promotional keyword: '{word}'")

        if review.count("!") >= 3:
            reasons.append("Too many exclamation marks (!!!)")

        if len(review.split()) <= 4:
            reasons.append("Review too short — may be spam")

        if reasons:
            st.write("🔎 Why it may be suspicious:")
            for r in reasons:
                st.write("•", r)
        else:
            st.write("✅ No suspicious patterns detected.")
