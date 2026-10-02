package com.mobilesales.analytics;

import com.mobilesales.model.AISearchData;
import java.util.HashMap;
import java.util.Map;

/**
 * EngagementAnalyzer calculates customer intent and engagement signals
 * from search volume, comparison queries, recommendation clicks, reviews, and repeat searches.
 */
public class EngagementAnalyzer {

    // Weight coefficients based on purchase-intent funnel:
    // Repeat searches (0.25) & Recommendations (0.20) indicate high conversion intent,
    // Comparisons (0.20) indicate mid-funnel evaluation,
    // Interactions (0.15) & Reviews (0.10) & Raw Searches (0.10) form baseline engagement.
    private static final double W_REPEAT = 0.25;
    private static final double W_RECOMMENDATION = 0.20;
    private static final double W_COMPARISON = 0.20;
    private static final double W_INTERACTIONS = 0.15;
    private static final double W_REVIEWS = 0.10;
    private static final double W_SEARCHES = 0.10;

    public double calculateEngagementScore(AISearchData data) {
        if (data == null) return 50.0;

        // Normalize each factor against standard benchmark thresholds
        double normSearches = Math.min(100.0, (data.getProductSearches() / 500.0) * 100.0);
        double normComp = Math.min(100.0, (data.getComparisonQueries() / 250.0) * 100.0);
        double normRec = Math.min(100.0, (data.getRecommendationQueries() / 200.0) * 100.0);
        double normInteractions = Math.min(100.0, (data.getUserInteractions() / 300.0) * 100.0);
        double normReviews = Math.min(100.0, (data.getPositiveReviews() / 150.0) * 100.0);
        double normRepeat = Math.min(100.0, (data.getRepeatSearches() / 150.0) * 100.0);

        double score = (normSearches * W_SEARCHES) +
                       (normComp * W_COMPARISON) +
                       (normRec * W_RECOMMENDATION) +
                       (normInteractions * W_INTERACTIONS) +
                       (normReviews * W_REVIEWS) +
                       (normRepeat * W_REPEAT);

        return Math.min(100.0, Math.max(10.0, Math.round(score * 10.0) / 10.0));
    }

    public Map<String, Object> analyzeEngagement(AISearchData data) {
        Map<String, Object> results = new HashMap<>();
        double score = calculateEngagementScore(data);

        results.put("engagementScore", score);
        results.put("productSearches", data.getProductSearches());
        results.put("comparisonQueries", data.getComparisonQueries());
        results.put("recommendationQueries", data.getRecommendationQueries());
        results.put("userInteractions", data.getUserInteractions());
        results.put("positiveReviews", data.getPositiveReviews());
        results.put("repeatSearches", data.getRepeatSearches());

        String sentiment;
        if (score >= 80.0) sentiment = "HIGH_ENGAGEMENT";
        else if (score >= 50.0) sentiment = "MODERATE_ENGAGEMENT";
        else sentiment = "LOW_ENGAGEMENT";
        results.put("engagementLevel", sentiment);

        return results;
    }
}
