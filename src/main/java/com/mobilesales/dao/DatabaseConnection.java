package com.mobilesales.dao;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Database Connection Manager for JDBC.
 * Supports MySQL as primary relational DB with seamless automatic SQLite fallback
 * to guarantee robust college project demonstration and evaluation.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;

    private String dbType = "UNKNOWN";
    private String url;
    private String username;
    private String password;
    private boolean isInitialized = false;

    private DatabaseConnection() {
        loadConfiguration();
        initializeDatabase();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.info("db.properties not found, using default configuration");
        }

        this.url = System.getProperty("db.url", props.getProperty("db.url", "jdbc:mysql://localhost:3306/mobile_sales_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
        this.username = System.getProperty("db.user", props.getProperty("db.user", "root"));
        this.password = System.getProperty("db.password", props.getProperty("db.password", "root"));
    }

    private void initializeDatabase() {
        // Attempt MySQL connection first
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                LOGGER.info("Successfully connected to MySQL database at: " + url);
                dbType = "MYSQL";
                isInitialized = true;
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "MySQL connection failed (" + e.getMessage() + "). Falling back to embedded SQLite for zero-downtime demonstration.");
        }

        // Fallback to SQLite
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = com.mobilesales.util.EnvConfig.get("DB_PATH", "data/mobile_sales_ai.db");
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            this.url = "jdbc:sqlite:" + dbPath;
            this.username = null;
            this.password = null;
            this.dbType = "SQLITE";

            try (Connection conn = DriverManager.getConnection(url)) {
                createSqliteTables(conn);
                LOGGER.info("Initialized embedded SQLite database successfully: " + url);
                isInitialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize embedded database", ex);
        }
    }

    private void createSqliteTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "username TEXT NOT NULL UNIQUE," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL," +
                    "role TEXT DEFAULT 'ANALYST'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS mobile_product (" +
                    "product_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "brand TEXT NOT NULL," +
                    "model_name TEXT NOT NULL," +
                    "category TEXT DEFAULT 'Smartphone'," +
                    "price REAL NOT NULL," +
                    "launch_date TEXT," +
                    "ram TEXT," +
                    "storage TEXT," +
                    "processor TEXT," +
                    "battery TEXT," +
                    "display TEXT," +
                    "camera TEXT," +
                    "os TEXT," +
                    "network TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                    "sales_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL," +
                    "sale_date TEXT NOT NULL," +
                    "units_sold INTEGER NOT NULL DEFAULT 0," +
                    "current_weekly_sales INTEGER DEFAULT 0," +
                    "current_monthly_sales INTEGER DEFAULT 0," +
                    "previous_month_sales INTEGER DEFAULT 0," +
                    "revenue REAL NOT NULL DEFAULT 0.0," +
                    "sales_growth_pct REAL DEFAULT 0.0," +
                    "sales_trend TEXT DEFAULT 'Stable'," +
                    "predicted_future_sales INTEGER DEFAULT 0," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (" +
                    "inventory_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL UNIQUE," +
                    "current_stock INTEGER NOT NULL DEFAULT 0," +
                    "safety_stock INTEGER NOT NULL DEFAULT 10," +
                    "reorder_level INTEGER NOT NULL DEFAULT 20," +
                    "lead_time INTEGER NOT NULL DEFAULT 7," +
                    "available_stock INTEGER NOT NULL DEFAULT 0," +
                    "inventory_status TEXT DEFAULT 'In Stock'," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS ai_search_data (" +
                    "search_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL UNIQUE," +
                    "total_queries INTEGER NOT NULL DEFAULT 0," +
                    "appearances INTEGER NOT NULL DEFAULT 0," +
                    "brand_mentions INTEGER NOT NULL DEFAULT 0," +
                    "competitor_comparisons INTEGER NOT NULL DEFAULT 0," +
                    "visibility_score REAL DEFAULT 0.0," +
                    "correct_claims INTEGER NOT NULL DEFAULT 0," +
                    "total_claims INTEGER NOT NULL DEFAULT 0," +
                    "accuracy_score REAL DEFAULT 0.0," +
                    "product_searches INTEGER DEFAULT 0," +
                    "comparison_queries INTEGER DEFAULT 0," +
                    "recommendation_queries INTEGER DEFAULT 0," +
                    "user_interactions INTEGER DEFAULT 0," +
                    "positive_reviews INTEGER DEFAULT 0," +
                    "repeat_searches INTEGER DEFAULT 0," +
                    "engagement_score REAL DEFAULT 0.0," +
                    "claim_details TEXT," +
                    "analyzed_at TEXT," +
                    "analysis_status TEXT DEFAULT 'SUCCESS'," +
                    "status_message TEXT," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id))");

            // Safe schema migrations for existing databases
            try { stmt.execute("ALTER TABLE ai_search_data ADD COLUMN analyzed_at TEXT"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE ai_search_data ADD COLUMN analysis_status TEXT DEFAULT 'SUCCESS'"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE ai_search_data ADD COLUMN status_message TEXT"); } catch (Exception ignored) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS analytics (" +
                    "analytics_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL UNIQUE," +
                    "visibility_score REAL DEFAULT 0.0," +
                    "accuracy_score REAL DEFAULT 0.0," +
                    "engagement_score REAL DEFAULT 0.0," +
                    "predicted_demand INTEGER DEFAULT 0," +
                    "demand_growth_pct REAL DEFAULT 0.0," +
                    "demand_trend TEXT DEFAULT 'Stable'," +
                    "classification TEXT DEFAULT 'Medium Moving'," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS recommendation (" +
                    "recommendation_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id INTEGER NOT NULL," +
                    "status TEXT NOT NULL," +
                    "reorder_quantity INTEGER NOT NULL DEFAULT 0," +
                    "current_stock INTEGER DEFAULT 0," +
                    "reorder_level INTEGER DEFAULT 0," +
                    "predicted_demand INTEGER DEFAULT 0," +
                    "reason TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id))");
        }
    }

    public Connection getConnection() throws SQLException {
        if ("MYSQL".equalsIgnoreCase(dbType)) {
            return DriverManager.getConnection(url, username, password);
        } else {
            return DriverManager.getConnection(url);
        }
    }

    public String getDbType() {
        return dbType;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
