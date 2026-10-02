package com.mobilesales.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobilesales.model.AISearchData;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for AI Search Intelligence data.
 */
public class AISearchDAO {

    private final DatabaseConnection dbConnection;
    private final Gson gson;

    public AISearchDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.gson = new Gson();
    }

    public AISearchData getAISearchDataByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM ai_search_data WHERE product_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AISearchData data = new AISearchData();
                    data.setSearchId(rs.getInt("search_id"));
                    data.setProductId(rs.getInt("product_id"));
                    data.setTotalQueries(rs.getInt("total_queries"));
                    data.setProductAppearances(rs.getInt("appearances"));
                    data.setBrandMentions(rs.getInt("brand_mentions"));
                    data.setCompetitorComparisons(rs.getInt("competitor_comparisons"));
                    data.setVisibilityScore(rs.getDouble("visibility_score"));
                    data.setCorrectClaims(rs.getInt("correct_claims"));
                    data.setTotalClaims(rs.getInt("total_claims"));
                    data.setAccuracyScore(rs.getDouble("accuracy_score"));
                    data.setProductSearches(rs.getInt("product_searches"));
                    data.setComparisonQueries(rs.getInt("comparison_queries"));
                    data.setRecommendationQueries(rs.getInt("recommendation_queries"));
                    data.setUserInteractions(rs.getInt("user_interactions"));
                    data.setPositiveReviews(rs.getInt("positive_reviews"));
                    data.setRepeatSearches(rs.getInt("repeat_searches"));
                    data.setEngagementScore(rs.getDouble("engagement_score"));

                    try {
                        data.setAnalyzedAt(rs.getString("analyzed_at"));
                        data.setAnalysisStatus(rs.getString("analysis_status"));
                        data.setStatusMessage(rs.getString("status_message"));
                    } catch (SQLException ignored) {}

                    String detailsJson = rs.getString("claim_details");
                    if (detailsJson != null && !detailsJson.trim().isEmpty()) {
                        Type listType = new TypeToken<List<AISearchData.ClaimComparison>>(){}.getType();
                        List<AISearchData.ClaimComparison> claims = gson.fromJson(detailsJson, listType);
                        data.setClaimComparisons(claims != null ? claims : new ArrayList<>());
                    }
                    return data;
                }
            }
        }
        return null;
    }

    public void addOrUpdateAISearchData(AISearchData data) throws SQLException {
        AISearchData existing = getAISearchDataByProductId(data.getProductId());
        String sql;
        if (existing == null) {
            sql = "INSERT INTO ai_search_data (product_id, total_queries, appearances, brand_mentions, " +
                  "competitor_comparisons, visibility_score, correct_claims, total_claims, accuracy_score, " +
                  "product_searches, comparison_queries, recommendation_queries, user_interactions, " +
                  "positive_reviews, repeat_searches, engagement_score, claim_details, analyzed_at, " +
                  "analysis_status, status_message) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE ai_search_data SET total_queries = ?, appearances = ?, brand_mentions = ?, " +
                  "competitor_comparisons = ?, visibility_score = ?, correct_claims = ?, total_claims = ?, " +
                  "accuracy_score = ?, product_searches = ?, comparison_queries = ?, recommendation_queries = ?, " +
                  "user_interactions = ?, positive_reviews = ?, repeat_searches = ?, engagement_score = ?, " +
                  "claim_details = ?, analyzed_at = ?, analysis_status = ?, status_message = ? WHERE product_id = ?";
        }

        String claimJson = gson.toJson(data.getClaimComparisons());

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (existing == null) {
                pstmt.setInt(1, data.getProductId());
                pstmt.setInt(2, data.getTotalQueries());
                pstmt.setInt(3, data.getProductAppearances());
                pstmt.setInt(4, data.getBrandMentions());
                pstmt.setInt(5, data.getCompetitorComparisons());
                pstmt.setDouble(6, data.getVisibilityScore());
                pstmt.setInt(7, data.getCorrectClaims());
                pstmt.setInt(8, data.getTotalClaims());
                pstmt.setDouble(9, data.getAccuracyScore());
                pstmt.setInt(10, data.getProductSearches());
                pstmt.setInt(11, data.getComparisonQueries());
                pstmt.setInt(12, data.getRecommendationQueries());
                pstmt.setInt(13, data.getUserInteractions());
                pstmt.setInt(14, data.getPositiveReviews());
                pstmt.setInt(15, data.getRepeatSearches());
                pstmt.setDouble(16, data.getEngagementScore());
                pstmt.setString(17, claimJson);
                pstmt.setString(18, data.getAnalyzedAt());
                pstmt.setString(19, data.getAnalysisStatus());
                pstmt.setString(20, data.getStatusMessage());
            } else {
                pstmt.setInt(1, data.getTotalQueries());
                pstmt.setInt(2, data.getProductAppearances());
                pstmt.setInt(3, data.getBrandMentions());
                pstmt.setInt(4, data.getCompetitorComparisons());
                pstmt.setDouble(5, data.getVisibilityScore());
                pstmt.setInt(6, data.getCorrectClaims());
                pstmt.setInt(7, data.getTotalClaims());
                pstmt.setDouble(8, data.getAccuracyScore());
                pstmt.setInt(9, data.getProductSearches());
                pstmt.setInt(10, data.getComparisonQueries());
                pstmt.setInt(11, data.getRecommendationQueries());
                pstmt.setInt(12, data.getUserInteractions());
                pstmt.setInt(13, data.getPositiveReviews());
                pstmt.setInt(14, data.getRepeatSearches());
                pstmt.setDouble(15, data.getEngagementScore());
                pstmt.setString(16, claimJson);
                pstmt.setString(17, data.getAnalyzedAt());
                pstmt.setString(18, data.getAnalysisStatus());
                pstmt.setString(19, data.getStatusMessage());
                pstmt.setInt(20, data.getProductId());
            }
            pstmt.executeUpdate();
        }
    }
}
