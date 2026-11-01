package com.mobilesales.dao;

import com.mobilesales.exception.InvalidProductException;
import com.mobilesales.model.MobileProduct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Mobile Products.
 */
public class MobileProductDAO {

    private final DatabaseConnection dbConnection;

    public MobileProductDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<String> getAllBrands() throws SQLException {
        List<String> brands = new ArrayList<>();
        String sql = "SELECT DISTINCT brand FROM mobile_product ORDER BY brand ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                brands.add(rs.getString("brand"));
            }
        }
        return brands;
    }

    public List<MobileProduct> getProductsByBrand(String brand) throws SQLException {
        List<MobileProduct> products = new ArrayList<>();
        String sql = "SELECT * FROM mobile_product WHERE LOWER(brand) = LOWER(?) ORDER BY model_name ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    public MobileProduct getProductById(int productId) throws SQLException {
        String sql = "SELECT * FROM mobile_product WHERE product_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }

    public List<MobileProduct> getAllProducts() throws SQLException {
        List<MobileProduct> products = new ArrayList<>();
        String sql = "SELECT * FROM mobile_product ORDER BY brand, model_name ASC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public MobileProduct findByBrandAndModel(String brand, String modelName) throws SQLException {
        if (brand == null || modelName == null) return null;
        String sql = "SELECT * FROM mobile_product WHERE LOWER(TRIM(brand)) = LOWER(TRIM(?)) AND LOWER(TRIM(model_name)) = LOWER(TRIM(?))";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, modelName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }

    public int updateProduct(MobileProduct product) throws SQLException {
        String sql = "UPDATE mobile_product SET category = ?, price = ?, ram = ?, storage = ?, processor = ?, " +
                     "battery = ?, display = ?, camera = ?, os = ?, network = ? WHERE product_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getCategory() != null ? product.getCategory() : "Smartphone");
            pstmt.setDouble(2, product.getPrice() > 0 ? product.getPrice() : 299.99);
            pstmt.setString(3, product.getRam());
            pstmt.setString(4, product.getStorage());
            pstmt.setString(5, product.getProcessor());
            pstmt.setString(6, product.getBattery());
            pstmt.setString(7, product.getDisplay());
            pstmt.setString(8, product.getCamera());
            pstmt.setString(9, product.getOs());
            pstmt.setString(10, product.getNetwork());
            pstmt.setInt(11, product.getProductId());
            pstmt.executeUpdate();
            return product.getProductId();
        }
    }

    public int addOrUpdateProduct(MobileProduct product) throws InvalidProductException, SQLException {
        MobileProduct existing = findByBrandAndModel(product.getBrand(), product.getModelName());
        if (existing != null) {
            product.setProductId(existing.getProductId());
            updateProduct(product);
            return existing.getProductId();
        } else {
            return addProduct(product);
        }
    }

    public int addProduct(MobileProduct product) throws InvalidProductException, SQLException {
        if (product.getBrand() == null || product.getBrand().trim().isEmpty() ||
            product.getModelName() == null || product.getModelName().trim().isEmpty()) {
            throw new InvalidProductException("Brand and Model Name are mandatory for adding a product.");
        }
        if (product.getPrice() <= 0) {
            product.setPrice(299.99);
        }

        String sql = "INSERT INTO mobile_product (brand, model_name, category, price, launch_date, ram, storage, processor, battery, display, camera, os, network) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, product.getBrand().trim());
            pstmt.setString(2, product.getModelName().trim());
            pstmt.setString(3, product.getCategory() != null ? product.getCategory() : "Smartphone");
            pstmt.setDouble(4, product.getPrice());
            pstmt.setString(5, product.getLaunchDate() != null ? product.getLaunchDate().toString() : LocalDate.now().toString());
            pstmt.setString(6, product.getRam());
            pstmt.setString(7, product.getStorage());
            pstmt.setString(8, product.getProcessor());
            pstmt.setString(9, product.getBattery());
            pstmt.setString(10, product.getDisplay());
            pstmt.setString(11, product.getCamera());
            pstmt.setString(12, product.getOs());
            pstmt.setString(13, product.getNetwork());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        product.setProductId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public List<MobileProduct> searchProducts(String query) throws SQLException {
        List<MobileProduct> products = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return products;
        }

        String cleanQuery = query.trim().replaceAll("\\s+", " ");
        String[] words = cleanQuery.split(" ");
        String fullPattern = "%" + cleanQuery.toLowerCase() + "%";

        StringBuilder whereSql = new StringBuilder();
        List<String> params = new ArrayList<>();

        // Multi-token matching: each word must match either brand, model_name, or category
        for (int i = 0; i < words.length; i++) {
            if (i > 0) whereSql.append(" AND ");
            whereSql.append("(LOWER(p.brand) LIKE ? OR LOWER(p.model_name) LIKE ? OR LOWER(p.category) LIKE ?)");
            String wPattern = "%" + words[i].toLowerCase() + "%";
            params.add(wPattern);
            params.add(wPattern);
            params.add(wPattern);
        }

        String sql = "SELECT p.*, " +
                "COALESCE(i.current_stock, 0) AS current_stock, " +
                "COALESCE(i.inventory_status, 'In Stock') AS inventory_status, " +
                "COALESCE(s.current_monthly_sales, 0) AS current_monthly_sales, " +
                "COALESCE(s.revenue, 0.0) AS revenue, " +
                "COALESCE(s.sales_trend, 'Stable') AS sales_trend " +
                "FROM mobile_product p " +
                "LEFT JOIN inventory i ON p.product_id = i.product_id " +
                "LEFT JOIN sales s ON p.product_id = s.product_id " +
                "WHERE " + whereSql + " " +
                "ORDER BY " +
                "  CASE " +
                "    WHEN LOWER(TRIM(p.model_name)) = LOWER(?) THEN 1 " +
                "    WHEN LOWER(TRIM(p.brand)) = LOWER(?) THEN 2 " +
                "    WHEN LOWER(p.model_name) LIKE LOWER(?) THEN 3 " +
                "    ELSE 4 " +
                "  END, p.brand ASC, p.model_name ASC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int pIndex = 1;
            for (String pVal : params) {
                pstmt.setString(pIndex++, pVal);
            }
            pstmt.setString(pIndex++, cleanQuery.toLowerCase());
            pstmt.setString(pIndex++, cleanQuery.toLowerCase());
            pstmt.setString(pIndex++, cleanQuery.toLowerCase() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    private MobileProduct mapResultSetToProduct(ResultSet rs) throws SQLException {
        MobileProduct p = new MobileProduct();
        p.setProductId(rs.getInt("product_id"));
        p.setBrand(rs.getString("brand"));
        p.setModelName(rs.getString("model_name"));
        p.setCategory(rs.getString("category"));
        p.setPrice(rs.getDouble("price"));
        String dateStr = rs.getString("launch_date");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                p.setLaunchDate(LocalDate.parse(dateStr.split(" ")[0]));
            } catch (Exception e) {
                p.setLaunchDate(LocalDate.now());
            }
        }
        p.setRam(rs.getString("ram"));
        p.setStorage(rs.getString("storage"));
        p.setProcessor(rs.getString("processor"));
        p.setBattery(rs.getString("battery"));
        p.setDisplay(rs.getString("display"));
        p.setCamera(rs.getString("camera"));
        p.setOs(rs.getString("os"));
        p.setNetwork(rs.getString("network"));

        // Enriched search & reporting fields if returned in joined query
        try {
            p.setCurrentStock(rs.getInt("current_stock"));
        } catch (SQLException ignored) {}
        try {
            String invStatus = rs.getString("inventory_status");
            if (invStatus != null) p.setInventoryStatus(invStatus);
        } catch (SQLException ignored) {}
        try {
            p.setMonthlySales(rs.getInt("current_monthly_sales"));
        } catch (SQLException ignored) {}
        try {
            p.setRevenue(rs.getDouble("revenue"));
        } catch (SQLException ignored) {}
        try {
            String trend = rs.getString("sales_trend");
            if (trend != null) p.setSalesTrend(trend);
        } catch (SQLException ignored) {}

        return p;
    }
}
