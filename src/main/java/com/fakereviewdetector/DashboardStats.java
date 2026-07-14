package com.fakereviewdetector;

record DashboardStats(
        int trainingSamples,
        int genuineSamples,
        int fakeSamples,
        int vocabularySize
) {
}
