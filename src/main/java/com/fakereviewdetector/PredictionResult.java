package com.fakereviewdetector;

import java.util.List;

record PredictionResult(
        String review,
        String label,
        int numericLabel,
        double confidence,
        double fakeProbability,
        String riskLevel,
        int wordCount,
        int characterCount,
        long exclamationCount,
        List<String> matchedKeywords,
        List<String> reasons
) {
}
