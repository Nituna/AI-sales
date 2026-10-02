package com.mobilesales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Access Object for Analytics aggregated records.
 */
public class AnalyticsDAO {

    private final DatabaseConnection dbConnection;

    public AnalyticsDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Map<String, Object> getAnalyticsByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM analytics WHERE product_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("analyticsId", rs.getInt("analytics_id"));
                    map.put("productId", rs.getInt("product_id"));
                    map.put("visibilityScore", rs.getDouble("visibility_score"));
                    map.put("accuracyScore", rs.getDouble("accuracy_score"));
                    map.put("engagementScore", rs.getDouble("engagement_score"));
                    map.put("predictedDemand", rs.getInt("predicted_demand"));
                    map.put("demandGrowthPct", rs.getDouble("demand_growth_pct"));
                    map.put("demandTrend", rs.getString("demand_trend"));
                    map.put("classification", rs.getString("classification"));
                    return map;
                }
            }
        }
        return null;
    }

    public void saveAnalytics(int productId, double visibility, double accuracy, double engagement,
                              int predictedDemand, double demandGrowthPct, String demandTrend, String classification) throws SQLException {
        Map<String, Object> existing = getAnalyticsByProductId(productId);
        String sql;
        if (existing == null) {
            sql = "INSERT INTO analytics (product_id, visibility_score, accuracy_score, engagement_score, " +
                  "predicted_demand, demand_growth_pct, demand_trend, classification) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE analytics SET visibility_score = ?, accuracy_score = ?, engagement_score = ?, " +
                  "predicted_demand = ?, demand_growth_pct = ?, demand_trend = ?, classification = ? WHERE product_id = ?";
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (existing == null) {
                pstmt.setInt(1, productId);
                pstmt.setDouble(2, visibility);
                pstmt.setDouble(3, accuracy);
                pstmt.setDouble(4, engagement);
                pstmt.setInt(5, predictedDemand);
                pstmt.setDouble(6, demandGrowthPct);
                pstmt.setString(7, demandTrend);
                pstmt.setString(8, classification);
            } else {
                pstmt.setDouble(1, visibility);
                pstmt.setDouble(2, accuracy);
                pstmt.setDouble(3, engagement);
                pstmt.setInt(4, predictedDemand);
                pstmt.setDouble(5, demandGrowthPct);
                pstmt.setString(6, demandTrend);
                pstmt.setString(7, classification);
                pstmt.setInt(8, productId);
            }
            pstmt.executeUpdate();
        }
    }
}
