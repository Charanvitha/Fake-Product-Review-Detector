package com.fakereviewdetector;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FakeReviewDetectorApp {
    private final FakeReviewClassifier classifier;
    private final DashboardStats dashboardStats;

    private FakeReviewDetectorApp(FakeReviewClassifier classifier, DashboardStats dashboardStats) {
        this.classifier = classifier;
        this.dashboardStats = dashboardStats;
    }

    public static void main(String[] args) throws IOException {
        List<ReviewSample> samples = CsvReviewLoader.load("reviews.csv");
        FakeReviewClassifier classifier = new FakeReviewClassifier();
        classifier.train(samples);

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        FakeReviewDetectorApp app = new FakeReviewDetectorApp(classifier, dashboardStats(samples, classifier));
        server.createContext("/", app::handleIndex);
        server.createContext("/predict", app::handlePredict);
        server.createContext("/batch", app::handleBatchPredict);
        server.createContext("/stats", app::handleStats);
        server.setExecutor(null);
        server.start();

        System.out.println("Fake Product Review Detector is running at http://localhost:" + port);
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain", "Method not allowed");
            return;
        }
        send(exchange, 200, "text/html", readResource("index.html"));
    }

    private void handlePredict(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String review = readFormValue(body, "review").trim();
        if (review.isEmpty()) {
            send(exchange, 400, "application/json", "{\"error\":\"Please enter a review first.\"}");
            return;
        }

        PredictionResult result = classifier.predict(review);
        send(exchange, 200, "application/json", predictionToJson(result));
    }

    private void handleBatchPredict(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String reviewsText = readFormValue(body, "reviews").trim();
        if (reviewsText.isEmpty()) {
            send(exchange, 400, "application/json", "{\"error\":\"Please enter at least one review.\"}");
            return;
        }

        List<PredictionResult> predictions = new ArrayList<>();
        for (String line : reviewsText.split("\\R")) {
            String review = line.trim();
            if (!review.isEmpty()) {
                predictions.add(classifier.predict(review));
            }
        }
        if (predictions.isEmpty()) {
            send(exchange, 400, "application/json", "{\"error\":\"Please enter at least one review.\"}");
            return;
        }

        long fakeCount = predictions.stream().filter(result -> result.numericLabel() == 1).count();
        String items = predictions.stream()
                .map(FakeReviewDetectorApp::predictionToJson)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        String json = """
                {
                  "totalReviews": %d,
                  "fakeReviews": %d,
                  "genuineReviews": %d,
                  "results": [%s]
                }
                """.formatted(predictions.size(), fakeCount, predictions.size() - fakeCount, items);
        send(exchange, 200, "application/json", json);
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }

        String json = """
                {
                  "trainingSamples": %d,
                  "genuineSamples": %d,
                  "fakeSamples": %d,
                  "vocabularySize": %d
                }
                """.formatted(
                dashboardStats.trainingSamples(),
                dashboardStats.genuineSamples(),
                dashboardStats.fakeSamples(),
                dashboardStats.vocabularySize()
        );
        send(exchange, 200, "application/json", json);
    }

    private static String readFormValue(String body, String key) {
        for (String pair : body.split("&")) {
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }
            String pairKey = decode(pair.substring(0, equalsIndex));
            if (pairKey.equals(key)) {
                return decode(pair.substring(equalsIndex + 1));
            }
        }
        return "";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String readResource(String resourceName) throws IOException {
        InputStream inputStream = FakeReviewDetectorApp.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IOException("Could not find resource: " + resourceName);
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String toJsonArray(List<String> values) {
        return values.stream()
                .map(FakeReviewDetectorApp::escapeJson)
                .map(value -> "\"" + value + "\"")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String predictionToJson(PredictionResult result) {
        return """
                {
                  "review": "%s",
                  "label": "%s",
                  "numericLabel": %d,
                  "confidence": %.2f,
                  "fakeProbability": %.2f,
                  "riskLevel": "%s",
                  "wordCount": %d,
                  "characterCount": %d,
                  "exclamationCount": %d,
                  "matchedKeywords": [%s],
                  "reasons": [%s]
                }
                """.formatted(
                escapeJson(result.review()),
                escapeJson(result.label()),
                result.numericLabel(),
                result.confidence(),
                result.fakeProbability(),
                escapeJson(result.riskLevel()),
                result.wordCount(),
                result.characterCount(),
                result.exclamationCount(),
                toJsonArray(result.matchedKeywords()),
                toJsonArray(result.reasons())
        );
    }

    private static DashboardStats dashboardStats(List<ReviewSample> samples, FakeReviewClassifier classifier) {
        int fakeSamples = 0;
        for (ReviewSample sample : samples) {
            if (sample.label() == 1) {
                fakeSamples++;
            }
        }
        return new DashboardStats(samples.size(), samples.size() - fakeSamples, fakeSamples, classifier.vocabularySize());
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
