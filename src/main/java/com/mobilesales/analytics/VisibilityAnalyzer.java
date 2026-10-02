package com.mobilesales.analytics;

import com.mobilesales.model.AISearchData;
import java.util.HashMap;
import java.util.Map;

/**
 * VisibilityAnalyzer evaluates product reach in AI and search queries.
 * Formula: Visibility = (Product appearances / Total relevant queries) * 100
 */
public class VisibilityAnalyzer {

    public double calculateVisibilityScore(int appearances, int totalQueries) {
        if (totalQueries <= 0) return 0.0;
        double score = ((double) appearances / totalQueries) * 100.0;
        return Math.min(100.0, Math.round(score * 10.0) / 10.0);
    }

    public double calculateBrandMentionRatio(int brandMentions, int totalQueries) {
        if (totalQueries <= 0) return 0.0;
        double ratio = ((double) brandMentions / totalQueries) * 100.0;
        return Math.min(100.0, Math.round(ratio * 10.0) / 10.0);
    }

    public double calculateCompetitorComparisonIndex(int competitorComparisons, int appearances) {
        if (appearances <= 0) return 0.0;
        double index = ((double) competitorComparisons / appearances) * 100.0;
        return Math.min(100.0, Math.round(index * 10.0) / 10.0);
    }

    public Map<String, Object> analyzeVisibility(AISearchData data) {
        Map<String, Object> results = new HashMap<>();
        double visibility = calculateVisibilityScore(data.getProductAppearances(), data.getTotalQueries());
        double brandRatio = calculateBrandMentionRatio(data.getBrandMentions(), data.getTotalQueries());
        double compIndex = calculateCompetitorComparisonIndex(data.getCompetitorComparisons(), data.getProductAppearances());

        results.put("visibilityScore", visibility);
        results.put("brandMentionRatio", brandRatio);
        results.put("competitorComparisonIndex", compIndex);
        results.put("totalQueries", data.getTotalQueries());
        results.put("appearances", data.getProductAppearances());
        results.put("brandMentions", data.getBrandMentions());
        results.put("competitorComparisons", data.getCompetitorComparisons());

        String status;
        if (visibility >= 80.0) status = "HIGH_VISIBILITY";
        else if (visibility >= 50.0) status = "MODERATE_VISIBILITY";
        else status = "LOW_VISIBILITY";
        results.put("visibilityCategory", status);

        return results;
    }
}
