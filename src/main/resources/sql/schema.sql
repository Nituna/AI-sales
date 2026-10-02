-- ========================================================
-- AI-Powered Mobile Inventory, Sales and Reorder Analytics System
-- Database Schema for MySQL 8.0+
-- ========================================================

CREATE DATABASE IF NOT EXISTS mobile_sales_ai;
USE mobile_sales_ai;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) DEFAULT 'ANALYST',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. MOBILE_PRODUCT TABLE
CREATE TABLE IF NOT EXISTS mobile_product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    brand VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) DEFAULT 'Smartphone',
    price DECIMAL(10, 2) NOT NULL,
    launch_date DATE,
    ram VARCHAR(20),
    storage VARCHAR(30),
    processor VARCHAR(100),
    battery VARCHAR(50),
    display VARCHAR(100),
    camera VARCHAR(100),
    os VARCHAR(50),
    network VARCHAR(50)
);

-- 3. SALES TABLE
CREATE TABLE IF NOT EXISTS sales (
    sales_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    sale_date DATE NOT NULL,
    units_sold INT NOT NULL DEFAULT 0,
    current_weekly_sales INT DEFAULT 0,
    current_monthly_sales INT DEFAULT 0,
    previous_month_sales INT DEFAULT 0,
    revenue DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    sales_growth_pct DECIMAL(6, 2) DEFAULT 0.00,
    sales_trend VARCHAR(30) DEFAULT 'Stable',
    predicted_future_sales INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE
);

-- 4. INVENTORY TABLE
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    current_stock INT NOT NULL DEFAULT 0,
    safety_stock INT NOT NULL DEFAULT 10,
    reorder_level INT NOT NULL DEFAULT 20,
    lead_time INT NOT NULL DEFAULT 7,
    available_stock INT NOT NULL DEFAULT 0,
    inventory_status VARCHAR(30) DEFAULT 'In Stock',
    FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE
);

-- 5. AI_SEARCH_DATA TABLE
CREATE TABLE IF NOT EXISTS ai_search_data (
    search_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    total_queries INT NOT NULL DEFAULT 0,
    appearances INT NOT NULL DEFAULT 0,
    brand_mentions INT NOT NULL DEFAULT 0,
    competitor_comparisons INT NOT NULL DEFAULT 0,
    visibility_score DECIMAL(5, 2) DEFAULT 0.00,
    correct_claims INT NOT NULL DEFAULT 0,
    total_claims INT NOT NULL DEFAULT 0,
    accuracy_score DECIMAL(5, 2) DEFAULT 0.00,
    product_searches INT DEFAULT 0,
    comparison_queries INT DEFAULT 0,
    recommendation_queries INT DEFAULT 0,
    user_interactions INT DEFAULT 0,
    positive_reviews INT DEFAULT 0,
    repeat_searches INT DEFAULT 0,
    engagement_score DECIMAL(5, 2) DEFAULT 0.00,
    claim_details TEXT,
    FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE
);

-- 6. ANALYTICS TABLE
CREATE TABLE IF NOT EXISTS analytics (
    analytics_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE,
    visibility_score DECIMAL(5, 2) DEFAULT 0.00,
    accuracy_score DECIMAL(5, 2) DEFAULT 0.00,
    engagement_score DECIMAL(5, 2) DEFAULT 0.00,
    predicted_demand INT DEFAULT 0,
    demand_growth_pct DECIMAL(6, 2) DEFAULT 0.00,
    demand_trend VARCHAR(50) DEFAULT 'Stable',
    classification VARCHAR(50) DEFAULT 'Medium Moving',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE
);

-- 7. RECOMMENDATION TABLE
CREATE TABLE IF NOT EXISTS recommendation (
    recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    reorder_quantity INT NOT NULL DEFAULT 0,
    current_stock INT DEFAULT 0,
    reorder_level INT DEFAULT 0,
    predicted_demand INT DEFAULT 0,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES mobile_product(product_id) ON DELETE CASCADE
);
