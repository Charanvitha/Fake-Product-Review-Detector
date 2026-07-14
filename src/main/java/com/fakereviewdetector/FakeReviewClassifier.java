package com.fakereviewdetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FakeReviewClassifier {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z']+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from",
            "has", "have", "in", "is", "it", "of", "on", "or", "that", "the", "this",
            "to", "was", "were", "with"
    );
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "best", "must buy", "amazing", "superb", "perfect", "unbelievable", "buy now"
    );

    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, Double> inverseDocumentFrequency = new HashMap<>();
    private double bias;

    void train(List<ReviewSample> samples) {
        buildVocabulary(samples);

        double learningRate = 0.45;
        int epochs = 1200;
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (ReviewSample sample : samples) {
                Map<String, Double> features = vectorize(sample.review());
                double predicted = sigmoid(score(features));
                double error = sample.label() - predicted;

                bias += learningRate * error;
                for (Map.Entry<String, Double> entry : features.entrySet()) {
                    weights.merge(entry.getKey(), learningRate * error * entry.getValue(), Double::sum);
                }
            }
            learningRate *= 0.997;
        }
    }

    PredictionResult predict(String review) {
        double fakeProbability = sigmoid(score(vectorize(review)));
        int numericLabel = fakeProbability >= 0.5 ? 1 : 0;
        String label = numericLabel == 1 ? "Fake" : "Genuine";
        double confidence = numericLabel == 1 ? fakeProbability * 100.0 : (1.0 - fakeProbability) * 100.0;
        List<String> matchedKeywords = matchedSuspiciousKeywords(review);
        return new PredictionResult(
                review,
                label,
                numericLabel,
                confidence,
                fakeProbability * 100.0,
                riskLevel(fakeProbability),
                wordCount(review),
                review.length(),
                exclamationCount(review),
                matchedKeywords,
                suspiciousReasons(review, matchedKeywords)
        );
    }

    int vocabularySize() {
        return inverseDocumentFrequency.size();
    }

    private void buildVocabulary(List<ReviewSample> samples) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (ReviewSample sample : samples) {
            Set<String> seen = new HashSet<>(tokens(sample.review()));
            for (String token : seen) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }

        int totalDocuments = samples.size();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            double idf = Math.log((1.0 + totalDocuments) / (1.0 + entry.getValue())) + 1.0;
            inverseDocumentFrequency.put(entry.getKey(), idf);
            weights.put(entry.getKey(), 0.0);
        }
    }

    private Map<String, Double> vectorize(String text) {
        Map<String, Integer> counts = new HashMap<>();
        List<String> tokens = tokens(text);
        for (String token : tokens) {
            if (inverseDocumentFrequency.containsKey(token)) {
                counts.merge(token, 1, Integer::sum);
            }
        }

        Map<String, Double> vector = new HashMap<>();
        int tokenCount = Math.max(tokens.size(), 1);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double tf = entry.getValue() / (double) tokenCount;
            vector.put(entry.getKey(), tf * inverseDocumentFrequency.get(entry.getKey()));
        }
        return vector;
    }

    private double score(Map<String, Double> features) {
        double result = bias;
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            result += weights.getOrDefault(entry.getKey(), 0.0) * entry.getValue();
        }
        return result;
    }

    private static double sigmoid(double value) {
        if (value >= 0) {
            double exp = Math.exp(-value);
            return 1.0 / (1.0 + exp);
        }
        double exp = Math.exp(value);
        return exp / (1.0 + exp);
    }

    private static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String riskLevel(double fakeProbability) {
        if (fakeProbability >= 0.75) {
            return "High";
        }
        if (fakeProbability >= 0.45) {
            return "Medium";
        }
        return "Low";
    }

    private static int wordCount(String review) {
        String trimmed = review.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static long exclamationCount(String review) {
        return review.chars().filter(ch -> ch == '!').count();
    }

    private static List<String> matchedSuspiciousKeywords(String review) {
        String lowerReview = review.toLowerCase(Locale.ROOT);
        List<String> matchedKeywords = new ArrayList<>();
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (lowerReview.contains(keyword)) {
                matchedKeywords.add(keyword);
            }
        }
        return matchedKeywords;
    }

    private static List<String> suspiciousReasons(String review, List<String> matchedKeywords) {
        List<String> reasons = new ArrayList<>();

        for (String keyword : matchedKeywords) {
            reasons.add("Contains promotional keyword: '" + keyword + "'");
        }
        if (exclamationCount(review) >= 3) {
            reasons.add("Too many exclamation marks (!!!)");
        }
        if (wordCount(review) <= 4) {
            reasons.add("Review too short - may be spam");
        }
        if (hasRepeatedWords(review)) {
            reasons.add("Repeated words detected");
        }
        if (review.equals(review.toUpperCase(Locale.ROOT)) && wordCount(review) > 2) {
            reasons.add("Mostly uppercase text can look promotional");
        }
        return reasons;
    }

    private static boolean hasRepeatedWords(String review) {
        String previous = "";
        int streak = 1;
        for (String token : tokens(review)) {
            if (token.equals(previous)) {
                streak++;
                if (streak >= 3) {
                    return true;
                }
            } else {
                streak = 1;
            }
            previous = token;
        }
        return false;
    }
}
