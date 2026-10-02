package com.mobilesales.dao;

import com.mobilesales.model.Inventory;
import com.mobilesales.model.InventoryStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for Inventory stock levels.
 */
public class InventoryDAO {

    private final DatabaseConnection dbConnection;

    public InventoryDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Inventory getInventoryByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM inventory WHERE product_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Inventory inv = new Inventory();
                    inv.setInventoryId(rs.getInt("inventory_id"));
                    inv.setProductId(rs.getInt("product_id"));
                    inv.setCurrentStock(rs.getInt("current_stock"));
                    inv.setSafetyStock(rs.getInt("safety_stock"));
                    inv.setReorderLevel(rs.getInt("reorder_level"));
                    inv.setSupplierLeadTimeDays(rs.getInt("lead_time"));
                    inv.setAvailableStock(rs.getInt("available_stock"));
                    inv.setInventoryStatus(InventoryStatus.fromString(rs.getString("inventory_status")));
                    return inv;
                }
            }
        }
        return null;
    }

    public void addOrUpdateInventory(Inventory inv) throws SQLException {
        Inventory existing = getInventoryByProductId(inv.getProductId());
        String sql;
        if (existing == null) {
            sql = "INSERT INTO inventory (product_id, current_stock, safety_stock, reorder_level, lead_time, available_stock, inventory_status) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE inventory SET current_stock = ?, safety_stock = ?, reorder_level = ?, " +
                  "lead_time = ?, available_stock = ?, inventory_status = ? WHERE product_id = ?";
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (existing == null) {
                pstmt.setInt(1, inv.getProductId());
                pstmt.setInt(2, inv.getCurrentStock());
                pstmt.setInt(3, inv.getSafetyStock());
                pstmt.setInt(4, inv.getReorderLevel());
                pstmt.setInt(5, inv.getSupplierLeadTimeDays());
                pstmt.setInt(6, inv.getAvailableStock());
                pstmt.setString(7, inv.getInventoryStatus() != null ? inv.getInventoryStatus().getDisplayName() : "In Stock");
            } else {
                pstmt.setInt(1, inv.getCurrentStock());
                pstmt.setInt(2, inv.getSafetyStock());
                pstmt.setInt(3, inv.getReorderLevel());
                pstmt.setInt(4, inv.getSupplierLeadTimeDays());
                pstmt.setInt(5, inv.getAvailableStock());
                pstmt.setString(6, inv.getInventoryStatus() != null ? inv.getInventoryStatus().getDisplayName() : "In Stock");
                pstmt.setInt(7, inv.getProductId());
            }
            pstmt.executeUpdate();
        }
    }
}
