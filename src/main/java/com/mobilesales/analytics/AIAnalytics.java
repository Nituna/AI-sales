package com.mobilesales.analytics;

import java.util.HashMap;
import java.util.Map;

import com.mobilesales.model.AISearchData;
import com.mobilesales.model.MobileProduct;

/**
 * AIAnalytics aggregates Visibility, Accuracy, and Customer Engagement
 * analyzers. Demonstrates Polymorphism and Composition.
 */
public class AIAnalytics implements Analytics<AISearchData, Map<String, Object>> {

    private final VisibilityAnalyzer visibilityAnalyzer;
    private final AccuracyAnalyzer accuracyAnalyzer;
    private final EngagementAnalyzer engagementAnalyzer;

    public AIAnalytics() {
        this.visibilityAnalyzer = new VisibilityAnalyzer();
        this.accuracyAnalyzer = new AccuracyAnalyzer();
        this.engagementAnalyzer = new EngagementAnalyzer();
    }

    @Override
    public String getAnalyticsName() {
        return "AI Search Intelligence & Sentiment Analytics";
    }

    @Override
    public Map<String, Object> analyze(AISearchData data) {
        Map<String, Object> combined = new HashMap<>();
        if (data == null) {
            return combined;
        }

        Map<String, Object> vis = visibilityAnalyzer.analyzeVisibility(data);
        Map<String, Object> eng = engagementAnalyzer.analyzeEngagement(data);

        combined.putAll(vis);
        combined.putAll(eng);
        combined.put("accuracyScore", data.getAccuracyScore());
        combined.put("correctClaims", data.getCorrectClaims());
        combined.put("totalClaims", data.getTotalClaims());
        combined.put("claimComparisons", data.getClaimComparisons());
        combined.put("analyzedAt", data.getAnalyzedAt());
        combined.put("analysisStatus", data.getAnalysisStatus() != null ? data.getAnalysisStatus() : "SUCCESS");
        combined.put("statusMessage", data.getStatusMessage());
        combined.put("engagementType", data.getEngagementType() != null ? data.getEngagementType() : "Engagement Proxy");

        return combined;
    }

    public Map<String, Object> analyzeWithProduct(AISearchData data, MobileProduct product) {
        Map<String, Object> combined = analyze(data);
        Map<String, Object> acc = accuracyAnalyzer.analyzeAccuracy(data, product);
        combined.putAll(acc);
        return combined;
    }

    @Override
    public Map<String, Object> getMetricsSummary(AISearchData data) {
        return analyze(data);
    }

    public VisibilityAnalyzer getVisibilityAnalyzer() {
        return visibilityAnalyzer;
    }

    public AccuracyAnalyzer getAccuracyAnalyzer() {
        return accuracyAnalyzer;
    }

    public EngagementAnalyzer getEngagementAnalyzer() {
        return engagementAnalyzer;
    }
}
