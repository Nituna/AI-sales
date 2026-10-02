package com.mobilesales;

import com.mobilesales.dao.DataInitializer;
import com.mobilesales.dao.DatabaseConnection;
import com.mobilesales.server.AppServer;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Main Entry Point for AI-Enhanced Mobile Inventory & Sales Analytics.
 * College Data Science + Java OOP Project.
 */
public class MainApplication {

    private static final Logger LOGGER = Logger.getLogger(MainApplication.class.getName());

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("   AI-POWERED MOBILE INVENTORY, SALES & REORDER ANALYTICS SYSTEM   ");
        System.out.println("         Data Science + Java Object-Oriented Programming (OOP)          ");
        System.out.println("==========================================================================");

        try {
            // 1. Initialize Relational Database via JDBC (MySQL with auto SQLite fallback)
            DatabaseConnection db = DatabaseConnection.getInstance();
            System.out.println("[INFO] Relational Database Initialized: " + db.getDbType());

            // 2. Seed Initial Catalog, Sales History & AI Search Intelligence
            DataInitializer.seedInitialData();
            System.out.println("[INFO] Baseline Catalog & Analytics Pipeline seeded.");

            // Check if GUI mode explicitly requested
            boolean launchGui = false;
            for (String arg : args) {
                if ("--gui".equalsIgnoreCase(arg) || "--swing".equalsIgnoreCase(arg)) {
                    launchGui = true;
                    break;
                }
            }

            if (!launchGui) {
                System.setProperty("java.awt.headless", "true");
            }

            // 3. Launch HTTP Server
            AppServer server = new AppServer();
            server.start();

            String url = "http://localhost:" + server.getPort();
            System.out.println("\n>>> [SUCCESS] Application started");
            System.out.println(">>> [SUCCESS] Web Application running at: " + url);
            System.out.println(">>> Default Login Credentials: username: 'admin', password: 'password123'");
            System.out.println(">>> (Or test with username: 'analyst', password: 'analyst123')\n");

            if (launchGui) {
                System.out.println("[INFO] Launching Java Swing Desktop GUI...");
                com.mobilesales.gui.MobileAnalyticsSwingApp.launchApp();
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Application failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
