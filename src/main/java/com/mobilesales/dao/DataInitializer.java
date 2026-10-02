package com.mobilesales.dao;

import com.mobilesales.analytics.AccuracyAnalyzer;
import com.mobilesales.analytics.DemandPrediction;
import com.mobilesales.analytics.ProductClassifier;
import com.mobilesales.analytics.RecommendationEngine;
import com.mobilesales.analytics.VisibilityAnalyzer;
import com.mobilesales.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DataInitializer seeds realistic initial datasets for Samsung, Apple, OnePlus,
 * Xiaomi, Vivo, Oppo, and Google Pixel.
 * Automatically run at application boot.
 */
public class DataInitializer {

    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    public static void seedInitialData() {
        try {
            UserDAO userDAO = new UserDAO();
            MobileProductDAO productDAO = new MobileProductDAO();
            SalesDAO salesDAO = new SalesDAO();
            InventoryDAO inventoryDAO = new InventoryDAO();
            AISearchDAO aiDAO = new AISearchDAO();
            AnalyticsDAO analyticsDAO = new AnalyticsDAO();
            RecommendationDAO recommendationDAO = new RecommendationDAO();

            // Seed default users if none exist
            if (!userDAO.existsByUsername("admin")) {
                userDAO.registerUser(new User("Admin Director", "admin", "admin@aisales.com", "password123"));
            }
            if (!userDAO.existsByUsername("analyst")) {
                userDAO.registerUser(new User("Data Analyst", "analyst", "analyst@aisales.com", "analyst123"));
            }
            if (!userDAO.existsByUsername("student")) {
                userDAO.registerUser(new User("Project Evaluator", "student", "student@college.edu", "student123"));
            }

            // Seed or update comprehensive smartphone catalog and analytics dataset
            List<MobileProduct> seedProducts = getSeedProducts();
            for (MobileProduct p : seedProducts) {
                MobileProduct existing = productDAO.findByBrandAndModel(p.getBrand(), p.getModelName());
                int pid;
                if (existing != null) {
                    pid = existing.getProductId();
                    p.setProductId(pid);
                    productDAO.updateProduct(p);
                } else {
                    pid = productDAO.addProduct(p);
                    p.setProductId(pid);
                }

                // Seed customized sales, inventory, and AI search data for each phone
                seedProductEcosystem(p, salesDAO, inventoryDAO, aiDAO, analyticsDAO, recommendationDAO);
            }
            LOGGER.info("Successfully synchronized " + seedProducts.size() + " smartphones across 7 brands with product-specific analytics.");
        } catch (Exception e) {
            LOGGER.severe("Error during database seeding: " + e.getMessage());
        }
    }

    private static void seedProductEcosystem(MobileProduct p, SalesDAO salesDAO, InventoryDAO inventoryDAO,
                                            AISearchDAO aiDAO, AnalyticsDAO analyticsDAO,
                                            RecommendationDAO recommendationDAO) throws SQLException {
        int pid = p.getProductId();
        String model = p.getModelName();
        String brand = p.getBrand();

        // Tune specific products to showcase different scenarios:
        // S24 Ultra: flagship high seller, stock tight
        // S25: upcoming/new, POTENTIAL FAST MOVING! Low current sales, high search & engagement!
        // iPhone 16 Pro Max: Fast moving, huge demand
        // Pixel 8a: Slow moving or moderate
        // OnePlus Nord 4: High volume mid-ranger

        int currentStock;
        int safetyStock;
        int reorderLvl;
        int leadTime = 7;
        int monthlySales;
        int prevMonthlySales;
        int weeklySales;
        int totalQueries;
        int appearances;
        int brandMentions;
        int compQueries;
        int productSearches;
        int recQueries;
        int interactions;
        int reviews;
        int repeatSearches;

        if (model.contains("S24 Ultra")) {
            currentStock = 38;
            safetyStock = 25;
            leadTime = 7;
            reorderLvl = 62;
            monthlySales = 160;
            prevMonthlySales = 130;
            weeklySales = 40;
            totalQueries = 1400;
            appearances = 1288; // 92%
            brandMentions = 1100;
            compQueries = 420;
            productSearches = 680;
            recQueries = 280;
            interactions = 390;
            reviews = 190;
            repeatSearches = 180;
        } else if (model.contains("S25")) {
            currentStock = 48;
            safetyStock = 20;
            leadTime = 7;
            reorderLvl = 30;
            monthlySales = 42;
            prevMonthlySales = 18;
            weeklySales = 14;
            totalQueries = 1800;
            appearances = 1656; // 92%
            brandMentions = 1450;
            compQueries = 550;
            productSearches = 820;
            recQueries = 360;
            interactions = 490;
            reviews = 180;
            repeatSearches = 240;
        } else if (model.contains("A56")) {
            currentStock = 140;
            safetyStock = 45;
            leadTime = 5;
            reorderLvl = 97;
            monthlySales = 310;
            prevMonthlySales = 280;
            weeklySales = 78;
            totalQueries = 920;
            appearances = 690; // 75%
            brandMentions = 540;
            compQueries = 220;
            productSearches = 380;
            recQueries = 160;
            interactions = 210;
            reviews = 95;
            repeatSearches = 85;
        } else if (model.contains("A36")) {
            currentStock = 85;
            safetyStock = 50;
            leadTime = 5;
            reorderLvl = 120;
            monthlySales = 420;
            prevMonthlySales = 390;
            weeklySales = 105;
            totalQueries = 780;
            appearances = 468; // 60%
            brandMentions = 360;
            compQueries = 160;
            productSearches = 280;
            recQueries = 110;
            interactions = 150;
            reviews = 70;
            repeatSearches = 60;
        } else if (model.contains("M55") || model.contains("M Series")) {
            currentStock = 190;
            safetyStock = 40;
            leadTime = 6;
            reorderLvl = 92;
            monthlySales = 260;
            prevMonthlySales = 275;
            weeklySales = 62;
            totalQueries = 640;
            appearances = 333; // 52%
            brandMentions = 240;
            compQueries = 110;
            productSearches = 190;
            recQueries = 80;
            interactions = 110;
            reviews = 55;
            repeatSearches = 45;
        } else if (model.contains("16 Pro Max")) {
            currentStock = 45;
            safetyStock = 80;
            leadTime = 8;
            reorderLvl = 219;
            monthlySales = 520;
            prevMonthlySales = 460;
            weeklySales = 135;
            totalQueries = 2800;
            appearances = 2660; // 95%
            brandMentions = 2300;
            compQueries = 850;
            productSearches = 1400;
            recQueries = 620;
            interactions = 820;
            reviews = 410;
            repeatSearches = 390;
        } else if (model.contains("iPhone 16")) {
            currentStock = 70;
            safetyStock = 60;
            leadTime = 7;
            reorderLvl = 149;
            monthlySales = 380;
            prevMonthlySales = 340;
            weeklySales = 98;
            totalQueries = 2100;
            appearances = 1848; // 88%
            brandMentions = 1580;
            compQueries = 580;
            productSearches = 960;
            recQueries = 410;
            interactions = 560;
            reviews = 270;
            repeatSearches = 260;
        } else if (model.contains("15 Pro")) {
            currentStock = 95;
            safetyStock = 40;
            leadTime = 7;
            reorderLvl = 89;
            monthlySales = 210;
            prevMonthlySales = 230;
            weeklySales = 52;
            totalQueries = 1350;
            appearances = 1107; // 82%
            brandMentions = 920;
            compQueries = 360;
            productSearches = 560;
            recQueries = 230;
            interactions = 320;
            reviews = 160;
            repeatSearches = 145;
        } else if (model.contains("iPhone 15")) {
            currentStock = 65;
            safetyStock = 75;
            leadTime = 7;
            reorderLvl = 190;
            monthlySales = 490;
            prevMonthlySales = 430;
            weeklySales = 125;
            totalQueries = 1950;
            appearances = 1677; // 86%
            brandMentions = 1420;
            compQueries = 520;
            productSearches = 890;
            recQueries = 380;
            interactions = 510;
            reviews = 250;
            repeatSearches = 240;
        } else if (model.contains("iPhone 14")) {
            currentStock = 120;
            safetyStock = 35;
            leadTime = 7;
            reorderLvl = 77;
            monthlySales = 180;
            prevMonthlySales = 210;
            weeklySales = 44;
            totalQueries = 890;
            appearances = 605; // 68%
            brandMentions = 480;
            compQueries = 190;
            productSearches = 330;
            recQueries = 130;
            interactions = 180;
            reviews = 90;
            repeatSearches = 80;
        } else if (model.contains("OnePlus 13")) {
            currentStock = 48;
            safetyStock = 20;
            leadTime = 7;
            reorderLvl = 45;
            monthlySales = 105;
            prevMonthlySales = 70;
            weeklySales = 28;
            totalQueries = 1250;
            appearances = 1062; // 85%
            brandMentions = 890;
            compQueries = 340;
            productSearches = 510;
            recQueries = 220;
            interactions = 290;
            reviews = 130;
            repeatSearches = 140;
        } else if (model.contains("OnePlus 12R")) {
            currentStock = 60;
            safetyStock = 45;
            leadTime = 6;
            reorderLvl = 103;
            monthlySales = 290;
            prevMonthlySales = 260;
            weeklySales = 74;
            totalQueries = 980;
            appearances = 745; // 76%
            brandMentions = 610;
            compQueries = 240;
            productSearches = 410;
            recQueries = 170;
            interactions = 230;
            reviews = 110;
            repeatSearches = 95;
        } else if (model.contains("Nord 4")) {
            currentStock = 110;
            safetyStock = 50;
            leadTime = 5;
            reorderLvl = 107;
            monthlySales = 340;
            prevMonthlySales = 310;
            weeklySales = 85;
            totalQueries = 820;
            appearances = 574; // 70%
            brandMentions = 430;
            compQueries = 180;
            productSearches = 320;
            recQueries = 140;
            interactions = 190;
            reviews = 85;
            repeatSearches = 75;
        } else if (model.contains("Find X8 Pro") || model.contains("Pixel 9 Pro XL")) {
            currentStock = 42;
            safetyStock = 25;
            leadTime = 7;
            reorderLvl = 47;
            monthlySales = 95;
            prevMonthlySales = 55;
            weeklySales = 26;
            totalQueries = 1500;
            appearances = 1350; // 90%
            brandMentions = 1120;
            compQueries = 440;
            productSearches = 620;
            recQueries = 270;
            interactions = 370;
            reviews = 150;
            repeatSearches = 190;
        } else {
            // Distinct hash-based deterministic metrics for all other models
            int h = Math.abs((brand + " " + model).hashCode());
            leadTime = 5 + (h % 5);
            monthlySales = 85 + (h % 260);
            prevMonthlySales = (int) Math.round(monthlySales * (0.80 + ((h % 30) / 100.0)));
            weeklySales = Math.max(1, (int) Math.round(monthlySales / 4.0));
            currentStock = 35 + (h % 110);
            safetyStock = Math.max(15, (int) Math.round(monthlySales * (leadTime / 30.0) * 0.8));
            reorderLvl = (int) Math.ceil(((monthlySales / 30.0) * leadTime) + safetyStock);
            totalQueries = 750 + (h % 1100);
            double vPct = 48.0 + (h % 45);
            appearances = (int) Math.round((vPct / 100.0) * totalQueries);
            brandMentions = (int) Math.round(appearances * 0.72);
            compQueries = (int) Math.round(appearances * 0.24);
            productSearches = (int) Math.round(appearances * 0.38);
            recQueries = (int) Math.round(appearances * 0.17);
            interactions = (int) Math.round(appearances * 0.21);
            reviews = (int) Math.round(appearances * 0.10);
            repeatSearches = (int) Math.round(appearances * 0.12);
        }

        double revenue = monthlySales * p.getPrice();
        double growth = ((double) (monthlySales - prevMonthlySales) / prevMonthlySales) * 100.0;
        growth = Math.round(growth * 10.0) / 10.0;
        String salesTrend = growth > 10.0 ? "Upward" : (growth < -5.0 ? "Downward" : "Stable");

        Sales s = new Sales(0, pid, LocalDate.now(), monthlySales * 8, weeklySales, monthlySales,
                prevMonthlySales, revenue, growth, salesTrend, (int)(monthlySales * 1.15));
        salesDAO.addOrUpdateSales(s);

        // Inventory
        InventoryStatus status;
        if (currentStock <= safetyStock) status = InventoryStatus.REORDER_REQUIRED;
        else if (currentStock <= reorderLvl) status = InventoryStatus.LOW_STOCK;
        else if (currentStock > (reorderLvl * 2.5)) status = InventoryStatus.OVERSTOCK;
        else status = InventoryStatus.IN_STOCK;

        Inventory inv = new Inventory(0, pid, currentStock, safetyStock, reorderLvl, leadTime, currentStock, status);
        inventoryDAO.addOrUpdateInventory(inv);

        // AI Search Data
        AISearchData ai = new AISearchData(0, pid, totalQueries, appearances, brandMentions,
                compQueries, 5, 5, productSearches, compQueries, recQueries, interactions, reviews, repeatSearches);

        // Build verified claim comparisons
        ai.addClaimComparison(new AISearchData.ClaimComparison("Battery Capacity", p.getBattery(), p.getBattery(), true, "AI Search Engine verified"));
        ai.addClaimComparison(new AISearchData.ClaimComparison("Processor Chipset", p.getProcessor(), p.getProcessor(), true, "Hardware knowledge graph"));
        ai.addClaimComparison(new AISearchData.ClaimComparison("Primary Camera", p.getCamera(), p.getCamera(), true, "Manufacturer official spec"));
        ai.addClaimComparison(new AISearchData.ClaimComparison("Display Panel", p.getDisplay(), p.getDisplay(), true, "Display tech benchmark"));
        ai.addClaimComparison(new AISearchData.ClaimComparison("RAM & Storage", p.getRam() + " / " + p.getStorage(), p.getRam() + " / " + p.getStorage(), true, "Retail specification index"));
        
        // Compute scores
        ai.setVisibilityScore(Math.round(((double) appearances / totalQueries * 100.0) * 10.0) / 10.0);
        ai.setAccuracyScore(100.0);
        double engScore = Math.min(100.0, Math.round(((repeatSearches * 0.25) + (recQueries * 0.20) + (compQueries * 0.20) + (interactions * 0.15) + (reviews * 0.10) + (productSearches * 0.10)) / 2.2));
        ai.setEngagementScore(engScore);
        aiDAO.addOrUpdateAISearchData(ai);

        // Run analytics engines
        DemandPrediction demandEngine = new DemandPrediction();
        DemandPredictionResult demandResult = demandEngine.predictDemand(s, inv, ai);

        ProductClassifier classifier = new ProductClassifier();
        ProductClassifier.ClassificationOutput classOutput = classifier.classifyProduct(s, ai);

        RecommendationEngine recEngine = new RecommendationEngine();
        ReorderRecommendation rec = recEngine.generateRecommendation(s, inv, ai, demandResult, classOutput);

        analyticsDAO.saveAnalytics(pid, ai.getVisibilityScore(), ai.getAccuracyScore(), ai.getEngagementScore(),
                demandResult.getPredictedDemand(), demandResult.getDemandGrowthPct(), demandResult.getDemandTrend(),
                classOutput.getClassification().getLabel());

        recommendationDAO.saveRecommendation(rec);
    }

    private static List<MobileProduct> getSeedProducts() {
        List<MobileProduct> list = new ArrayList<>();

        // Samsung
        list.add(new MobileProduct(0, "Samsung", "Galaxy S24 Ultra", "Flagship", 1299.99, LocalDate.of(2024, 1, 24),
                "12GB", "512GB", "Snapdragon 8 Gen 3 for Galaxy", "5000 mAh", "6.8\" Dynamic AMOLED 2X 120Hz", "200MP + 50MP + 12MP + 10MP", "Android 14 / One UI 6.1", "5G"));
        list.add(new MobileProduct(0, "Samsung", "Galaxy S25", "Flagship", 899.99, LocalDate.of(2025, 2, 10),
                "12GB", "256GB", "Snapdragon 8 Elite", "4000 mAh", "6.2\" Dynamic AMOLED 2X 120Hz", "50MP + 10MP + 12MP", "Android 15 / One UI 7", "5G"));
        list.add(new MobileProduct(0, "Samsung", "Galaxy A56", "Mid-Range", 449.99, LocalDate.of(2025, 3, 15),
                "8GB", "256GB", "Exynos 1580", "5000 mAh", "6.6\" Super AMOLED 120Hz", "50MP OIS + 12MP + 5MP", "Android 15 / One UI 7", "5G"));
        list.add(new MobileProduct(0, "Samsung", "Galaxy A36", "Budget Mid-Range", 329.99, LocalDate.of(2025, 3, 20),
                "6GB", "128GB", "Snapdragon 6 Gen 3", "5000 mAh", "6.5\" Super AMOLED 120Hz", "50MP + 8MP + 2MP", "Android 15 / One UI 7", "5G"));
        list.add(new MobileProduct(0, "Samsung", "Galaxy M Series (M55)", "Budget", 289.99, LocalDate.of(2024, 4, 10),
                "8GB", "128GB", "Snapdragon 7 Gen 1", "5000 mAh", "6.7\" Super AMOLED+ 120Hz", "50MP OIS + 8MP + 2MP", "Android 14 / One UI 6", "5G"));

        // Apple
        list.add(new MobileProduct(0, "Apple", "iPhone 16 Pro Max", "Flagship", 1199.99, LocalDate.of(2024, 9, 20),
                "8GB", "256GB", "Apple A18 Pro", "4685 mAh", "6.9\" Super Retina XDR OLED ProMotion", "48MP Fusion + 48MP Ultra-Wide + 12MP 5x Telephoto", "iOS 18", "5G"));
        list.add(new MobileProduct(0, "Apple", "iPhone 16", "Premium", 799.99, LocalDate.of(2024, 9, 20),
                "8GB", "128GB", "Apple A18", "3561 mAh", "6.1\" Super Retina XDR OLED", "48MP Fusion + 12MP Ultra-Wide", "iOS 18", "5G"));
        list.add(new MobileProduct(0, "Apple", "iPhone 15 Pro", "Flagship", 999.99, LocalDate.of(2023, 9, 22),
                "8GB", "128GB", "Apple A17 Pro", "3274 mAh", "6.1\" Super Retina XDR OLED ProMotion", "48MP Main + 12MP Ultra-Wide + 12MP 3x", "iOS 17", "5G"));
        list.add(new MobileProduct(0, "Apple", "iPhone 15", "Premium", 699.99, LocalDate.of(2023, 9, 22),
                "6GB", "128GB", "Apple A16 Bionic", "3349 mAh", "6.1\" Super Retina XDR OLED", "48MP Main + 12MP Ultra-Wide", "iOS 17", "5G"));
        list.add(new MobileProduct(0, "Apple", "iPhone 14", "Standard", 599.99, LocalDate.of(2022, 9, 16),
                "6GB", "128GB", "Apple A15 Bionic", "3279 mAh", "6.1\" Super Retina XDR OLED", "12MP Main + 12MP Ultra-Wide", "iOS 16", "5G"));

        // OnePlus
        list.add(new MobileProduct(0, "OnePlus", "OnePlus 13", "Flagship", 899.99, LocalDate.of(2025, 1, 10),
                "16GB", "512GB", "Snapdragon 8 Elite", "6000 mAh", "6.82\" 2K LTPO AMOLED 120Hz", "50MP Hasselblad + 50MP + 50MP Periscope", "OxygenOS 15", "5G"));
        list.add(new MobileProduct(0, "OnePlus", "OnePlus 12", "Flagship", 799.99, LocalDate.of(2024, 1, 23),
                "12GB", "256GB", "Snapdragon 8 Gen 3", "5400 mAh", "6.82\" 2K AMOLED ProXDR 120Hz", "50MP Sony LYT-808 + 64MP 3x + 48MP", "OxygenOS 14", "5G"));
        list.add(new MobileProduct(0, "OnePlus", "OnePlus 12R", "Performance", 499.99, LocalDate.of(2024, 2, 6),
                "16GB", "256GB", "Snapdragon 8 Gen 2", "5500 mAh", "6.78\" 1.5K AMOLED 120Hz", "50MP Sony IMX890 + 8MP + 2MP", "OxygenOS 14", "5G"));
        list.add(new MobileProduct(0, "OnePlus", "OnePlus Nord 4", "Mid-Range", 379.99, LocalDate.of(2024, 7, 16),
                "8GB", "256GB", "Snapdragon 7+ Gen 3", "5500 mAh", "6.74\" AMOLED 120Hz", "50MP Sony LYT-600 OIS + 8MP", "OxygenOS 14.1", "5G"));

        // Xiaomi
        list.add(new MobileProduct(0, "Xiaomi", "Xiaomi 15 Pro", "Flagship", 849.99, LocalDate.of(2024, 10, 29),
                "16GB", "512GB", "Snapdragon 8 Elite", "6100 mAh", "6.73\" 2K OLED 120Hz", "50MP Leica Main + 50MP 5x + 50MP UW", "HyperOS 2.0", "5G"));
        list.add(new MobileProduct(0, "Xiaomi", "Xiaomi 14 Ultra", "Ultra Flagship", 1299.99, LocalDate.of(2024, 3, 19),
                "16GB", "512GB", "Snapdragon 8 Gen 3", "5000 mAh", "6.73\" LTPO AMOLED 120Hz", "Quad 50MP Leica 1-inch sensor", "HyperOS", "5G"));
        list.add(new MobileProduct(0, "Xiaomi", "Redmi Note 14 Pro+", "Mid-Range", 349.99, LocalDate.of(2024, 9, 26),
                "12GB", "256GB", "Snapdragon 7s Gen 3", "6200 mAh", "6.67\" 1.5K Curved OLED 120Hz", "50MP Light Hunter 800 + 50MP 2.5x + 8MP", "HyperOS", "5G"));
        list.add(new MobileProduct(0, "Xiaomi", "POCO F6", "Performance", 369.99, LocalDate.of(2024, 5, 23),
                "12GB", "512GB", "Snapdragon 8s Gen 3", "5000 mAh", "6.67\" CrystalRes AMOLED 120Hz", "50MP Sony IMX882 OIS + 8MP", "HyperOS", "5G"));

        // Vivo
        list.add(new MobileProduct(0, "Vivo", "Vivo X100 Pro", "Flagship", 999.99, LocalDate.of(2024, 1, 4),
                "16GB", "512GB", "MediaTek Dimensity 9300", "5400 mAh", "6.78\" LTPO AMOLED 120Hz", "50MP ZEISS 1-inch + 50MP APO Tele + 50MP UW", "Funtouch OS 14", "5G"));
        list.add(new MobileProduct(0, "Vivo", "Vivo V40 Pro", "Portrait Flagship", 599.99, LocalDate.of(2024, 8, 7),
                "12GB", "512GB", "MediaTek Dimensity 9200+", "5500 mAh", "6.78\" 1.5K Curved AMOLED 120Hz", "Triple 50MP ZEISS Optics", "Funtouch OS 14", "5G"));
        list.add(new MobileProduct(0, "Vivo", "Vivo T3 Ultra", "Mid-Range", 379.99, LocalDate.of(2024, 9, 12),
                "8GB", "256GB", "MediaTek Dimensity 9200+", "5500 mAh", "6.78\" 3D Curved AMOLED 120Hz", "50MP Sony IMX921 OIS + 8MP", "Funtouch OS 14", "5G"));
        list.add(new MobileProduct(0, "Vivo", "Vivo Y200", "Budget Mid-Range", 249.99, LocalDate.of(2023, 10, 25),
                "8GB", "128GB", "Snapdragon 4 Gen 1", "4800 mAh", "6.67\" AMOLED 120Hz Smart Aura Light", "64MP OIS + 2MP", "Funtouch OS 13", "5G"));

        // Oppo
        list.add(new MobileProduct(0, "Oppo", "Find X8 Pro", "Flagship", 1099.99, LocalDate.of(2024, 11, 21),
                "16GB", "512GB", "MediaTek Dimensity 9400", "5910 mAh", "6.78\" Quad Curved AMOLED 120Hz", "50MP Hasselblad Dual Periscope Quad Camera", "ColorOS 15", "5G"));
        list.add(new MobileProduct(0, "Oppo", "Reno 12 Pro", "Design Mid-Range", 479.99, LocalDate.of(2024, 6, 18),
                "12GB", "512GB", "MediaTek Dimensity 7300-Energy", "5000 mAh", "6.7\" Quad Curved AMOLED 120Hz", "50MP Sony LYT-600 OIS + 50MP 2x + 8MP", "ColorOS 14.1", "5G"));
        list.add(new MobileProduct(0, "Oppo", "F27 Pro+", "Durable Mid-Range", 349.99, LocalDate.of(2024, 6, 13),
                "8GB", "256GB", "MediaTek Dimensity 7050", "5000 mAh", "6.7\" 3D Curved AMOLED 120Hz (IP69)", "64MP + 2MP", "ColorOS 14", "5G"));
        list.add(new MobileProduct(0, "Oppo", "A3 Pro", "Budget", 219.99, LocalDate.of(2024, 4, 12),
                "8GB", "128GB", "MediaTek Dimensity 7050", "5000 mAh", "6.7\" Curved OLED 120Hz", "64MP + 2MP", "ColorOS 14", "5G"));

        // Google Pixel
        list.add(new MobileProduct(0, "Google Pixel", "Pixel 9 Pro XL", "Flagship AI", 1099.99, LocalDate.of(2024, 8, 22),
                "16GB", "256GB", "Google Tensor G4 + Titan M2", "5060 mAh", "6.8\" Super Actua LTPO OLED 120Hz", "50MP Octa PD + 48MP Quad PD 5x + 48MP UW", "Android 15 (Gemini Nano Built-in)", "5G"));
        list.add(new MobileProduct(0, "Google Pixel", "Pixel 9", "AI Standard", 799.99, LocalDate.of(2024, 8, 22),
                "12GB", "128GB", "Google Tensor G4", "4700 mAh", "6.3\" Actua OLED 120Hz", "50MP Octa PD + 48MP Quad PD UW", "Android 15 (Gemini Nano Built-in)", "5G"));
        list.add(new MobileProduct(0, "Google Pixel", "Pixel 8a", "Mid-Range AI", 499.99, LocalDate.of(2024, 5, 14),
                "8GB", "128GB", "Google Tensor G3", "4492 mAh", "6.1\" Actua OLED 120Hz", "64MP Quad PD + 13MP Ultrawide", "Android 14 (7 yrs updates)", "5G"));
        list.add(new MobileProduct(0, "Google Pixel", "Pixel 8 Pro", "Flagship", 899.99, LocalDate.of(2023, 10, 12),
                "12GB", "256GB", "Google Tensor G3", "5050 mAh", "6.7\" Super Actua LTPO OLED 120Hz", "50MP Octa PD + 48MP 5x + 48MP UW", "Android 14", "5G"));

        return list;
    }
}
