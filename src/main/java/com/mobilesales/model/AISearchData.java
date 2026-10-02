package com.mobilesales.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing AI Search Intelligence signals:
 * 1. Visibility (appearances in search/AI queries)
 * 2. Information Accuracy (verified specs vs search claims)
 * 3. Customer Engagement (searches, comparisons, recommendations, reviews, repeat queries)
 */
public class AISearchData implements Serializable {
    private static final long serialVersionUID = 1L;

    private int searchId;
    private int productId;
    
    // Visibility components
    private int totalQueries;
    private int productAppearances;
    private int brandMentions;
    private int competitorComparisons;
    private double visibilityScore; // percentage 0-100%

    // Accuracy components
    private int correctClaims;
    private int totalClaims;
    private double accuracyScore; // percentage 0-100%
    private List<ClaimComparison> claimComparisons;

    // Engagement components
    private int productSearches;
    private int comparisonQueries;
    private int recommendationQueries;
    private int userInteractions;
    private int positiveReviews;
    private int repeatSearches;
    private double engagementScore; // percentage 0-100%

    // Real-Time Analysis Tracking & Status
    private String analyzedAt;
    private String analysisStatus = "SUCCESS"; // "SUCCESS", "UNAVAILABLE", "ERROR"
    private String statusMessage;
    private String engagementType = "Engagement Proxy";

    public AISearchData() {
        this.claimComparisons = new ArrayList<>();
    }

    public AISearchData(int searchId, int productId, int totalQueries, int productAppearances,
                        int brandMentions, int competitorComparisons, int correctClaims,
                        int totalClaims, int productSearches, int comparisonQueries,
                        int recommendationQueries, int userInteractions, int positiveReviews,
                        int repeatSearches) {
        this.searchId = searchId;
        this.productId = productId;
        this.totalQueries = totalQueries;
        this.productAppearances = productAppearances;
        this.brandMentions = brandMentions;
        this.competitorComparisons = competitorComparisons;
        this.correctClaims = correctClaims;
        this.totalClaims = totalClaims;
        this.productSearches = productSearches;
        this.comparisonQueries = comparisonQueries;
        this.recommendationQueries = recommendationQueries;
        this.userInteractions = userInteractions;
        this.positiveReviews = positiveReviews;
        this.repeatSearches = repeatSearches;
        this.claimComparisons = new ArrayList<>();
        
        // Auto compute default scores
        this.visibilityScore = totalQueries > 0 ? (double) productAppearances / totalQueries * 100.0 : 0.0;
        this.accuracyScore = totalClaims > 0 ? (double) correctClaims / totalClaims * 100.0 : 0.0;
    }

    // Inner class for spec claims breakdown
    public static class ClaimComparison implements Serializable {
        private static final long serialVersionUID = 1L;
        private String feature;
        private String verifiedValue;
        private String aiClaimedValue;
        private boolean isCorrect;
        private String sourceSnippet;

        public ClaimComparison() {}

        public ClaimComparison(String feature, String verifiedValue, String aiClaimedValue, boolean isCorrect, String sourceSnippet) {
            this.feature = feature;
            this.verifiedValue = verifiedValue;
            this.aiClaimedValue = aiClaimedValue;
            this.isCorrect = isCorrect;
            this.sourceSnippet = sourceSnippet;
        }

        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }
        public String getVerifiedValue() { return verifiedValue; }
        public void setVerifiedValue(String verifiedValue) { this.verifiedValue = verifiedValue; }
        public String getAiClaimedValue() { return aiClaimedValue; }
        public void setAiClaimedValue(String aiClaimedValue) { this.aiClaimedValue = aiClaimedValue; }
        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean correct) { isCorrect = correct; }
        public String getSourceSnippet() { return sourceSnippet; }
        public void setSourceSnippet(String sourceSnippet) { this.sourceSnippet = sourceSnippet; }
    }

    public int getSearchId() {
        return searchId;
    }

    public void setSearchId(int searchId) {
        this.searchId = searchId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(int totalQueries) {
        this.totalQueries = totalQueries;
    }

    public int getProductAppearances() {
        return productAppearances;
    }

    public void setProductAppearances(int productAppearances) {
        this.productAppearances = productAppearances;
    }

    public int getBrandMentions() {
        return brandMentions;
    }

    public void setBrandMentions(int brandMentions) {
        this.brandMentions = brandMentions;
    }

    public int getCompetitorComparisons() {
        return competitorComparisons;
    }

    public void setCompetitorComparisons(int competitorComparisons) {
        this.competitorComparisons = competitorComparisons;
    }

    public double getVisibilityScore() {
        return visibilityScore;
    }

    public void setVisibilityScore(double visibilityScore) {
        this.visibilityScore = visibilityScore;
    }

    public int getCorrectClaims() {
        return correctClaims;
    }

    public void setCorrectClaims(int correctClaims) {
        this.correctClaims = correctClaims;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(int totalClaims) {
        this.totalClaims = totalClaims;
    }

    public double getAccuracyScore() {
        return accuracyScore;
    }

    public void setAccuracyScore(double accuracyScore) {
        this.accuracyScore = accuracyScore;
    }

    public List<ClaimComparison> getClaimComparisons() {
        return claimComparisons;
    }

    public void setClaimComparisons(List<ClaimComparison> claimComparisons) {
        this.claimComparisons = claimComparisons;
    }

    public void addClaimComparison(ClaimComparison comparison) {
        this.claimComparisons.add(comparison);
    }

    public int getProductSearches() {
        return productSearches;
    }

    public void setProductSearches(int productSearches) {
        this.productSearches = productSearches;
    }

    public int getComparisonQueries() {
        return comparisonQueries;
    }

    public void setComparisonQueries(int comparisonQueries) {
        this.comparisonQueries = comparisonQueries;
    }

    public int getRecommendationQueries() {
        return recommendationQueries;
    }

    public void setRecommendationQueries(int recommendationQueries) {
        this.recommendationQueries = recommendationQueries;
    }

    public int getUserInteractions() {
        return userInteractions;
    }

    public void setUserInteractions(int userInteractions) {
        this.userInteractions = userInteractions;
    }

    public int getPositiveReviews() {
        return positiveReviews;
    }

    public void setPositiveReviews(int positiveReviews) {
        this.positiveReviews = positiveReviews;
    }

    public int getRepeatSearches() {
        return repeatSearches;
    }

    public void setRepeatSearches(int repeatSearches) {
        this.repeatSearches = repeatSearches;
    }

    public double getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(double engagementScore) {
        this.engagementScore = engagementScore;
    }

    public String getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(String analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
    }
}
