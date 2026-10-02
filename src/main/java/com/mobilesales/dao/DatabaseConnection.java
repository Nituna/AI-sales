package com.mobilesales.dao;

import com.mobilesales.util.EnvConfig;
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

        // Support environment variables: DB_URL, DATABASE_URL, MYSQL_URL, DB_USER, DB_PASSWORD
        String rawUrl = EnvConfig.get("DB_URL");
        if (rawUrl == null || rawUrl.isEmpty()) {
            rawUrl = EnvConfig.get("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isEmpty()) {
            rawUrl = EnvConfig.get("MYSQL_URL");
        }

        String userFromUrl = null;
        String passFromUrl = null;

        if (rawUrl != null && !rawUrl.isEmpty()) {
            // Handle cloud database URL formats, e.g. mysql://user:password@host:port/database
            if (rawUrl.startsWith("mysql://")) {
                String remainder = rawUrl.substring("mysql://".length());
                if (remainder.contains("@")) {
                    int atIdx = remainder.indexOf('@');
                    String userPass = remainder.substring(0, atIdx);
                    String hostAndDb = remainder.substring(atIdx + 1);
                    if (userPass.contains(":")) {
                        String[] parts = userPass.split(":", 2);
                        userFromUrl = parts[0];
                        passFromUrl = parts[1];
                    } else {
                        userFromUrl = userPass;
                    }
                    rawUrl = "jdbc:mysql://" + hostAndDb;
                } else {
                    rawUrl = "jdbc:mysql://" + remainder;
                }
            }
        } else {
            rawUrl = System.getProperty("db.url", props.getProperty("db.url", "jdbc:mysql://localhost:3306/mobile_sales_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000&socketTimeout=3000"));
        }

        this.url = rawUrl;
        this.username = EnvConfig.get("DB_USER", EnvConfig.get("DB_USERNAME", userFromUrl != null ? userFromUrl : System.getProperty("db.user", props.getProperty("db.user", "root"))));
        this.password = EnvConfig.get("DB_PASSWORD", passFromUrl != null ? passFromUrl : System.getProperty("db.password", props.getProperty("db.password", "root")));
    }

    private void initializeDatabase() {
        String dbTypeConfig = EnvConfig.get("DB_TYPE", System.getProperty("db.type", "")).trim();

        // If explicitly set to SQLITE, initialize SQLite immediately without attempting MySQL
        if ("SQLITE".equalsIgnoreCase(dbTypeConfig) || (this.url != null && this.url.startsWith("jdbc:sqlite:"))) {
            initSqlite();
            return;
        }

        // Attempt MySQL connection first (short timeout prevents long hangs if MySQL is unavailable)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(3);
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                createMysqlTables(conn);
                LOGGER.info("Successfully connected to MySQL database at: " + url);
                dbType = "MYSQL";
                isInitialized = true;
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "MySQL connection failed (" + e.getMessage() + "). Falling back to embedded SQLite for zero-downtime demonstration.");
        }

        // Fallback to SQLite
        initSqlite();
    }

    private void initSqlite() {
        try {
            Class.forName("org.sqlite.JDBC");
            String sqlitePath = EnvConfig.get("SQLITE_DB_PATH", "data/mobile_sales_ai.db");
            File dbFile = new File(sqlitePath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            this.url = "jdbc:sqlite:" + sqlitePath;
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

    private void createMysqlTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "username VARCHAR(50) NOT NULL UNIQUE," +
                    "email VARCHAR(100) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "role VARCHAR(30) DEFAULT 'ANALYST'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS mobile_product (" +
                    "product_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "brand VARCHAR(50) NOT NULL," +
                    "model_name VARCHAR(100) NOT NULL," +
                    "category VARCHAR(50) DEFAULT 'Smartphone'," +
                    "price DECIMAL(10, 2) NOT NULL," +
                    "launch_date DATE," +
                    "ram VARCHAR(20)," +
                    "storage VARCHAR(30)," +
                    "processor VARCHAR(100)," +
                    "battery VARCHAR(50)," +
                    "display VARCHAR(100)," +
                    "camera VARCHAR(100)," +
                    "os VARCHAR(50)," +
                    "network VARCHAR(50))");

            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                    "sales_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "product_id INT NOT NULL," +
                    "sale_date DATE NOT NULL," +
                    "units_sold INT NOT NULL DEFAULT 0," +
                    "current_weekly_sales INT DEFAULT 0," +
                    "current_monthly_sales INT DEFAULT 0," +
                    "previous_month_sales INT DEFAULT 0," +
                    "revenue DECIMAL(12, 2) NOT NULL DEFAULT 0.00," +
                    "sales_growth_pct DECIMAL(6, 2) DEFAULT 0.00," +
                    "sales_trend VARCHAR(30) DEFAULT 'Stable'," +
                    "predicted_future_sales INT DEFAULT 0," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (" +
                    "inventory_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "product_id INT NOT NULL UNIQUE," +
                    "current_stock INT NOT NULL DEFAULT 0," +
                    "safety_stock INT NOT NULL DEFAULT 10," +
                    "reorder_level INT NOT NULL DEFAULT 20," +
                    "lead_time INT NOT NULL DEFAULT 7," +
                    "available_stock INT NOT NULL DEFAULT 0," +
                    "inventory_status VARCHAR(30) DEFAULT 'In Stock'," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS ai_search_data (" +
                    "search_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "product_id INT NOT NULL UNIQUE," +
                    "total_queries INT NOT NULL DEFAULT 0," +
                    "appearances INT NOT NULL DEFAULT 0," +
                    "brand_mentions INT NOT NULL DEFAULT 0," +
                    "competitor_comparisons INT NOT NULL DEFAULT 0," +
                    "visibility_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "correct_claims INT NOT NULL DEFAULT 0," +
                    "total_claims INT NOT NULL DEFAULT 0," +
                    "accuracy_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "product_searches INT DEFAULT 0," +
                    "comparison_queries INT DEFAULT 0," +
                    "recommendation_queries INT DEFAULT 0," +
                    "user_interactions INT DEFAULT 0," +
                    "positive_reviews INT DEFAULT 0," +
                    "repeat_searches INT DEFAULT 0," +
                    "engagement_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "claim_details TEXT," +
                    "analyzed_at VARCHAR(100)," +
                    "analysis_status VARCHAR(50) DEFAULT 'SUCCESS'," +
                    "status_message TEXT," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS analytics (" +
                    "analytics_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "product_id INT NOT NULL UNIQUE," +
                    "visibility_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "accuracy_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "engagement_score DECIMAL(5, 2) DEFAULT 0.00," +
                    "predicted_demand INT DEFAULT 0," +
                    "demand_growth_pct DECIMAL(6, 2) DEFAULT 0.00," +
                    "demand_trend VARCHAR(50) DEFAULT 'Stable'," +
                    "classification VARCHAR(50) DEFAULT 'Medium Moving'," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS recommendation (" +
                    "recommendation_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "product_id INT NOT NULL," +
                    "status VARCHAR(50) NOT NULL," +
                    "reorder_quantity INT NOT NULL DEFAULT 0," +
                    "current_stock INT DEFAULT 0," +
                    "reorder_level INT DEFAULT 0," +
                    "predicted_demand INT DEFAULT 0," +
                    "reason TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE)");
        } catch (Exception e) {
            LOGGER.warning("Note on MySQL table check/creation: " + e.getMessage());
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
