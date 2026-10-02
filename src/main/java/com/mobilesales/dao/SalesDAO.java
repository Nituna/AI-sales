package com.mobilesales.dao;

import com.mobilesales.model.Sales;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Sales records.
 */
public class SalesDAO {

    private final DatabaseConnection dbConnection;

    public SalesDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public Sales getSalesByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM sales WHERE product_id = ? ORDER BY sales_id DESC LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Sales s = new Sales();
                    s.setSalesId(rs.getInt("sales_id"));
                    s.setProductId(rs.getInt("product_id"));
                    String dateStr = rs.getString("sale_date");
                    if (dateStr != null) {
                        try { s.setRecordDate(LocalDate.parse(dateStr.split(" ")[0])); } catch (Exception ignored) {}
                    }
                    s.setTotalUnitsSold(rs.getInt("units_sold"));
                    s.setCurrentWeeklySales(rs.getInt("current_weekly_sales"));
                    s.setCurrentMonthlySales(rs.getInt("current_monthly_sales"));
                    s.setPreviousMonthSales(rs.getInt("previous_month_sales"));
                    s.setRevenue(rs.getDouble("revenue"));
                    s.setSalesGrowthPct(rs.getDouble("sales_growth_pct"));
                    s.setSalesTrend(rs.getString("sales_trend"));
                    s.setPredictedFutureSales(rs.getInt("predicted_future_sales"));

                    // Generate realistic weekly and monthly series for charts
                    int monthly = s.getCurrentMonthlySales();
                    int prev = s.getPreviousMonthSales();
                    int base = (int) (monthly / 4.0);
                    List<Integer> weeks = new ArrayList<>();
                    weeks.add(Math.max(2, base - 6));
                    weeks.add(Math.max(3, base - 3));
                    weeks.add(Math.max(4, base + 2));
                    weeks.add(s.getCurrentWeeklySales() > 0 ? s.getCurrentWeeklySales() : base + 4);
                    s.setWeeklyHistory(weeks);

                    List<Integer> months = new ArrayList<>();
                    months.add((int) (prev * 0.82));
                    months.add((int) (prev * 0.90));
                    months.add(prev);
                    months.add(monthly);
                    s.setMonthlyHistory(months);

                    return s;
                }
            }
        }
        return null;
    }

    public void addOrUpdateSales(Sales sales) throws SQLException {
        Sales existing = getSalesByProductId(sales.getProductId());
        String sql;
        if (existing == null) {
            sql = "INSERT INTO sales (product_id, sale_date, units_sold, current_weekly_sales, " +
                  "current_monthly_sales, previous_month_sales, revenue, sales_growth_pct, sales_trend, predicted_future_sales) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE sales SET sale_date = ?, units_sold = ?, current_weekly_sales = ?, " +
                  "current_monthly_sales = ?, previous_month_sales = ?, revenue = ?, sales_growth_pct = ?, " +
                  "sales_trend = ?, predicted_future_sales = ? WHERE product_id = ?";
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (existing == null) {
                pstmt.setInt(1, sales.getProductId());
                pstmt.setString(2, sales.getRecordDate() != null ? sales.getRecordDate().toString() : LocalDate.now().toString());
                pstmt.setInt(3, sales.getTotalUnitsSold());
                pstmt.setInt(4, sales.getCurrentWeeklySales());
                pstmt.setInt(5, sales.getCurrentMonthlySales());
                pstmt.setInt(6, sales.getPreviousMonthSales());
                pstmt.setDouble(7, sales.getRevenue());
                pstmt.setDouble(8, sales.getSalesGrowthPct());
                pstmt.setString(9, sales.getSalesTrend());
                pstmt.setInt(10, sales.getPredictedFutureSales());
            } else {
                pstmt.setString(1, sales.getRecordDate() != null ? sales.getRecordDate().toString() : LocalDate.now().toString());
                pstmt.setInt(2, sales.getTotalUnitsSold());
                pstmt.setInt(3, sales.getCurrentWeeklySales());
                pstmt.setInt(4, sales.getCurrentMonthlySales());
                pstmt.setInt(5, sales.getPreviousMonthSales());
                pstmt.setDouble(6, sales.getRevenue());
                pstmt.setDouble(7, sales.getSalesGrowthPct());
                pstmt.setString(8, sales.getSalesTrend());
                pstmt.setInt(9, sales.getPredictedFutureSales());
                pstmt.setInt(10, sales.getProductId());
            }
            pstmt.executeUpdate();
        }
    }
}
