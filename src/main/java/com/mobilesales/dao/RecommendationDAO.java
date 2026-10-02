package com.mobilesales.dao;

import com.mobilesales.model.ReorderRecommendation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for Smart Reorder Recommendations.
 */
public class RecommendationDAO {

    private final DatabaseConnection dbConnection;

    public RecommendationDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public ReorderRecommendation getRecommendationByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM recommendation WHERE product_id = ? ORDER BY recommendation_id DESC LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ReorderRecommendation rec = new ReorderRecommendation();
                    rec.setRecommendationId(rs.getInt("recommendation_id"));
                    rec.setProductId(rs.getInt("product_id"));
                    rec.setStatus(rs.getString("status"));
                    rec.setRecommendedReorderQty(rs.getInt("reorder_quantity"));
                    rec.setCurrentStock(rs.getInt("current_stock"));
                    rec.setReorderLevel(rs.getInt("reorder_level"));
                    rec.setPredictedDemand(rs.getInt("predicted_demand"));

                    String reasonsRaw = rs.getString("reason");
                    if (reasonsRaw != null && !reasonsRaw.trim().isEmpty()) {
                        rec.setReasonsList(new ArrayList<>(Arrays.asList(reasonsRaw.split(";;"))));
                    }
                    return rec;
                }
            }
        }
        return null;
    }

    public void saveRecommendation(ReorderRecommendation rec) throws SQLException {
        String sql = "INSERT INTO recommendation (product_id, status, reorder_quantity, current_stock, " +
                     "reorder_level, predicted_demand, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rec.getProductId());
            pstmt.setString(2, rec.getStatus());
            pstmt.setInt(3, rec.getRecommendedReorderQty());
            pstmt.setInt(4, rec.getCurrentStock());
            pstmt.setInt(5, rec.getReorderLevel());
            pstmt.setInt(6, rec.getPredictedDemand());

            String reasons = String.join(";;", rec.getReasonsList());
            pstmt.setString(7, reasons);

            pstmt.executeUpdate();
        }
    }
}
