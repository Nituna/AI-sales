package com.mobilesales.server;

import com.google.gson.*;
import com.mobilesales.analytics.*;
import com.mobilesales.dao.*;
import com.mobilesales.exception.DataValidationException;
import com.mobilesales.exception.InvalidLoginException;
import com.mobilesales.exception.InvalidProductException;
import com.mobilesales.io.CSVDataExporter;
import com.mobilesales.io.CSVDataImporter;
import com.mobilesales.io.ReportGenerator;
import com.mobilesales.model.*;
import com.mobilesales.service.AISearchEngine;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * Embedded Lightweight Java HTTP Server.
 * Serves the modern Dark Glassmorphic Web App and provides full REST API endpoints.
 * Demonstrates Java Networking, Multi-threading, and REST Architecture.
 */
public class AppServer {

    private static final Logger LOGGER = Logger.getLogger(AppServer.class.getName());
    private int port = 8080;

    private final HttpServer server;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src != null ? src.toString() : ""))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> {
                try {
                    return LocalDate.parse(json.getAsString().split(" ")[0]);
                } catch (Exception e) {
                    return LocalDate.now();
                }
            })
            .create();

    private final UserDAO userDAO = new UserDAO();
    private final MobileProductDAO productDAO = new MobileProductDAO();
    private final SalesDAO salesDAO = new SalesDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AISearchDAO aiDAO = new AISearchDAO();
    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();

    private final SalesAnalytics salesAnalytics = new SalesAnalytics();
    private final InventoryAnalytics inventoryAnalytics = new InventoryAnalytics();
    private final AIAnalytics aiAnalytics = new AIAnalytics();
    private final DemandPrediction demandPrediction = new DemandPrediction();
    private final ProductClassifier productClassifier = new ProductClassifier();
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();
    private final AISearchEngine aiSearchEngine = new AISearchEngine();
    private volatile String lastImportErrorsCsv = null;

    public AppServer() throws IOException {
        String envPort = System.getenv("PORT");
        if (envPort == null || envPort.trim().isEmpty()) {
            envPort = com.mobilesales.util.EnvConfig.get("PORT", null);
        }

        HttpServer createdServer = null;
        if (envPort != null && !envPort.trim().isEmpty()) {
            int p;
            try {
                p = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid PORT environment variable value '" + envPort + "', falling back to 8080.");
                p = 8080;
            }
            this.port = p;
            createdServer = HttpServer.create(new InetSocketAddress("0.0.0.0", this.port), 0);
            LOGGER.info("Configured server to listen on PORT environment variable: " + this.port);
        } else {
            // Local fallback: try 8080, then 8081, 8082, etc.
            int[] trialPorts = new int[]{8080, 8081, 8082, 8085, 8090, 8888, 9000};
            IOException lastEx = null;
            for (int p : trialPorts) {
                try {
                    createdServer = HttpServer.create(new InetSocketAddress("0.0.0.0", p), 0);
                    this.port = p;
                    break;
                } catch (IOException e) {
                    lastEx = e;
                    LOGGER.warning("Port " + p + " is occupied, attempting next available port...");
                }
            }
            if (createdServer == null) {
                throw lastEx != null ? lastEx : new IOException("Could not bind to any available port.");
            }
        }

        this.server = createdServer;
        setupRoutes();
        this.server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    }

    public int getPort() {
        return port;
    }

    public void start() {
        server.start();
        LOGGER.info("AI Mobile Inventory & Sales Analytics Server started on 0.0.0.0:" + port);
        LOGGER.info("Web Application: http://0.0.0.0:" + port + " (local: http://localhost:" + port + ")");
        LOGGER.info("Database: " + DatabaseConnection.getInstance().getDbType());
    }

    public void stop() {
        server.stop(0);
        LOGGER.info("Server stopped.");
    }

    private void setupRoutes() {
        // Health Check Route for Render & Monitoring
        server.createContext("/health", new HealthHandler());

        // API Routes
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/brands", new BrandsHandler());
        server.createContext("/api/products", new ProductsHandler());
        server.createContext("/api/products/search", new ProductSearchHandler());
        server.createContext("/api/product/dashboard", new ProductDashboardHandler());
        server.createContext("/api/ai-search/refresh", new AISearchRefreshHandler());
        server.createContext("/api/main-dashboard", new MainDashboardHandler());
        server.createContext("/api/products/add", new AddProductHandler());
        server.createContext("/api/csv/upload", new CsvUploadHandler());
        server.createContext("/api/csv/export", new CsvExportHandler());
        server.createContext("/api/csv/sample", new CsvSampleHandler());
        server.createContext("/api/csv/errors/download", new CsvErrorsDownloadHandler());
        server.createContext("/api/report/download", new ReportDownloadHandler());

        // Static Asset Handler
        server.createContext("/", new StaticFileHandler());
    }

    // ==========================================
    // HTTP HANDLERS
    // ==========================================

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ok");
            resp.put("timestamp", System.currentTimeMillis());
            resp.put("dbType", DatabaseConnection.getInstance().getDbType());
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                String username = json.has("username") ? json.get("username").getAsString() : "";
                String password = json.has("password") ? json.get("password").getAsString() : "";

                User user = userDAO.authenticate(username, password);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Authentication successful.");
                resp.put("user", Map.of(
                        "userId", user.getUserId(),
                        "name", user.getName(),
                        "username", user.getUsername(),
                        "role", user.getRole()
                ));
                sendJsonResponse(exchange, 200, resp);

            } catch (InvalidLoginException e) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("error", e.getMessage());
                sendJsonResponse(exchange, 401, resp);
            } catch (Exception e) {
                LOGGER.severe("Login error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", "Database error: " + e.getMessage()));
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                String name = json.has("name") ? json.get("name").getAsString() : "";
                String username = json.has("username") ? json.get("username").getAsString() : "";
                String email = json.has("email") ? json.get("email").getAsString() : "";
                String password = json.has("password") ? json.get("password").getAsString() : "";
                String confirmPassword = json.has("confirmPassword") ? json.get("confirmPassword").getAsString() : "";

                if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    throw new DataValidationException("All registration fields are required.");
                }
                if (!password.equals(confirmPassword)) {
                    throw new DataValidationException("Password and Confirm Password do not match.");
                }
                if (!email.contains("@") || !email.contains(".")) {
                    throw new DataValidationException("Please enter a valid email address.");
                }

                User user = new User(name, username, email, password);
                userDAO.registerUser(user);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Registration successful. You can now log in.");
                sendJsonResponse(exchange, 200, resp);

            } catch (DataValidationException e) {
                sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
            } catch (Exception e) {
                LOGGER.severe("Register error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", "Registration error: " + e.getMessage()));
            }
        }
    }

    private class BrandsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<String> brands = productDAO.getAllBrands();
                sendJsonResponse(exchange, 200, Map.of("success", true, "brands", brands));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String search = query.get("search");
                if (search == null || search.isEmpty()) search = query.get("q");
                if (search == null || search.isEmpty()) search = query.get("query");
                String brand = query.get("brand");

                List<MobileProduct> products;
                if (search != null && !search.trim().isEmpty()) {
                    products = productDAO.searchProducts(search.trim());
                } else if (brand != null && !brand.trim().isEmpty()) {
                    products = productDAO.getProductsByBrand(brand);
                } else {
                    products = productDAO.getAllProducts();
                }
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "count", products.size(),
                        "products", products
                ));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class ProductSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String q = query.get("q");
                if (q == null || q.isEmpty()) q = query.get("query");
                if (q == null || q.isEmpty()) q = query.get("search");

                if (q == null || q.trim().isEmpty()) {
                    sendJsonResponse(exchange, 200, Map.of("success", true, "count", 0, "products", List.of()));
                    return;
                }

                List<MobileProduct> products = productDAO.searchProducts(q.trim());
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "query", q.trim(),
                        "count", products.size(),
                        "products", products
                ));
            } catch (Exception e) {
                LOGGER.severe("Product search error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class ProductDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String pidStr = query.get("productId");
                if (pidStr == null || pidStr.isEmpty()) {
                    sendJsonResponse(exchange, 400, Map.of("success", false, "error", "productId query param required"));
                    return;
                }
                int productId = Integer.parseInt(pidStr);

                MobileProduct product = productDAO.getProductById(productId);
                if (product == null) {
                    sendJsonResponse(exchange, 404, Map.of("success", false, "error", "Product not found"));
                    return;
                }

                Sales sales = salesDAO.getSalesByProductId(productId);
                Inventory inventory = inventoryDAO.getInventoryByProductId(productId);
                AISearchData aiData = aiDAO.getAISearchDataByProductId(productId);

                // If no real-time analysis exists yet, perform real-time analysis
                if (aiData == null || aiData.getAnalyzedAt() == null || aiData.getAnalyzedAt().isEmpty()) {
                    try {
                        aiData = aiSearchEngine.analyzeProduct(product, false);
                    } catch (Exception ex) {
                        LOGGER.warning("Could not perform real-time AI search analysis: " + ex.getMessage());
                    }
                }

                // Run Analytics Modules
                Map<String, Object> salesMetrics = salesAnalytics.analyze(sales);
                Map<String, Object> invMetrics = inventoryAnalytics.analyze(inventory);
                Map<String, Object> aiMetrics = aiAnalytics.analyzeWithProduct(aiData, product);

                DemandPredictionResult demandResult = demandPrediction.predictDemand(sales, inventory, aiData);
                ProductClassifier.ClassificationOutput classification = productClassifier.classifyProduct(sales, aiData);
                ReorderRecommendation recommendation = recommendationEngine.generateRecommendation(
                        sales, inventory, aiData, demandResult, classification);

                // Build consolidated dashboard payload
                Map<String, Object> dashboard = new HashMap<>();
                dashboard.put("product", product);
                dashboard.put("sales", salesMetrics);
                dashboard.put("inventory", invMetrics);
                dashboard.put("aiSearch", aiMetrics);
                dashboard.put("demand", demandResult);
                dashboard.put("classification", Map.of(
                        "name", classification.getClassification().getLabel(),
                        "description", classification.getClassification().getDescription(),
                        "color", classification.getClassification().getColor(),
                        "icon", classification.getClassification().getIcon(),
                        "systemMessage", classification.getSystemMessage(),
                        "rationale", classification.getRationale(),
                        "isAlertTriggered", classification.isAlertTriggered()
                ));
                dashboard.put("recommendation", recommendation);

                sendJsonResponse(exchange, 200, Map.of("success", true, "dashboard", dashboard));

            } catch (Exception e) {
                LOGGER.severe("Product dashboard error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class AISearchRefreshHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String pidStr = query.get("productId");
                if (pidStr == null || pidStr.isEmpty()) {
                    sendJsonResponse(exchange, 400, Map.of("success", false, "error", "productId parameter required"));
                    return;
                }
                int productId = Integer.parseInt(pidStr);
                MobileProduct product = productDAO.getProductById(productId);
                if (product == null) {
                    sendJsonResponse(exchange, 404, Map.of("success", false, "error", "Product not found"));
                    return;
                }

                // Force fresh live AI/search retrieval & analysis
                AISearchData aiData = aiSearchEngine.analyzeProduct(product, true);
                Map<String, Object> aiMetrics = aiAnalytics.analyzeWithProduct(aiData, product);

                // Re-evaluate demand & recommendation with fresh AI signals
                Sales sales = salesDAO.getSalesByProductId(productId);
                Inventory inventory = inventoryDAO.getInventoryByProductId(productId);
                DemandPredictionResult demandResult = demandPrediction.predictDemand(sales, inventory, aiData);
                ProductClassifier.ClassificationOutput classification = productClassifier.classifyProduct(sales, aiData);
                ReorderRecommendation recommendation = recommendationEngine.generateRecommendation(
                        sales, inventory, aiData, demandResult, classification);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("aiSearch", aiMetrics);
                resp.put("demand", demandResult);
                resp.put("recommendation", recommendation);
                resp.put("message", "Real-time AI search analysis updated.");
                sendJsonResponse(exchange, 200, resp);

            } catch (Exception e) {
                LOGGER.severe("AI search refresh error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", "AI analysis temporarily unavailable: " + e.getMessage()));
            }
        }
    }

    private class MainDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<MobileProduct> products = productDAO.getAllProducts();

                int totalModels = products.size();
                int totalUnitsSold = 0;
                int totalStock = 0;
                int reordersRequired = 0;
                double totalVis = 0.0;
                double totalAcc = 0.0;
                double totalEng = 0.0;

                List<Map<String, Object>> productSummaries = new ArrayList<>();
                List<Map<String, Object>> alerts = new ArrayList<>();
                Map<String, Integer> brandSales = new HashMap<>();
                Map<String, Integer> classificationCounts = new HashMap<>();

                for (MobileProduct p : products) {
                    int pid = p.getProductId();
                    Sales s = salesDAO.getSalesByProductId(pid);
                    Inventory inv = inventoryDAO.getInventoryByProductId(pid);
                    AISearchData ai = aiDAO.getAISearchDataByProductId(pid);

                    DemandPredictionResult demandResult = demandPrediction.predictDemand(s, inv, ai);
                    ProductClassifier.ClassificationOutput classification = productClassifier.classifyProduct(s, ai);
                    ReorderRecommendation rec = recommendationEngine.generateRecommendation(s, inv, ai, demandResult, classification);

                    int curSales = s != null ? s.getCurrentMonthlySales() : 0;
                    int curStock = inv != null ? inv.getCurrentStock() : 0;
                    double vis = ai != null ? ai.getVisibilityScore() : 0.0;
                    double acc = ai != null ? ai.getAccuracyScore() : 100.0;
                    double eng = ai != null ? ai.getEngagementScore() : 0.0;

                    totalUnitsSold += (s != null ? s.getTotalUnitsSold() : 0);
                    totalStock += curStock;
                    totalVis += vis;
                    totalAcc += acc;
                    totalEng += eng;

                    brandSales.put(p.getBrand(), brandSales.getOrDefault(p.getBrand(), 0) + curSales);
                    String className = classification.getClassification().getLabel();
                    classificationCounts.put(className, classificationCounts.getOrDefault(className, 0) + 1);

                    if ("REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus())) {
                        reordersRequired++;
                    }

                    // Alerts Generation
                    if (curStock <= (inv != null ? inv.getSafetyStock() : 15)) {
                        alerts.add(Map.of(
                                "type", "LOW_STOCK",
                                "badge", "🔴 Low Stock",
                                "color", "#ef4444",
                                "message", p.getBrand() + " " + p.getModelName() + " stock (" + curStock + ") is below critical safety threshold."
                        ));
                    }
                    if ("REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus())) {
                        alerts.add(Map.of(
                                "type", "REORDER_REQUIRED",
                                "badge", "🟠 Reorder Required",
                                "color", "#f97316",
                                "message", p.getBrand() + " " + p.getModelName() + ": Recommended reorder of " + rec.getRecommendedReorderQty() + " units."
                        ));
                    }
                    if (classification.getClassification() == ProductClassification.POTENTIAL_FAST_MOVING) {
                        alerts.add(Map.of(
                                "type", "POTENTIAL_DEMAND_INCREASE",
                                "badge", "🟡 Potential Demand Increase",
                                "color", "#eab308",
                                "message", p.getBrand() + " " + p.getModelName() + ": High search engagement detected ahead of sales surge."
                        ));
                    }
                    if (vis < 50.0) {
                        alerts.add(Map.of(
                                "type", "LOW_VISIBILITY",
                                "badge", "🔵 Low Visibility",
                                "color", "#3b82f6",
                                "message", p.getBrand() + " " + p.getModelName() + " has low search presence (" + Math.round(vis) + "%)."
                        ));
                    }
                    if (inv != null && inv.getInventoryStatus() == InventoryStatus.OVERSTOCK) {
                        alerts.add(Map.of(
                                "type", "OVERSTOCK",
                                "badge", "⚠ Overstock",
                                "color", "#8b5cf6",
                                "message", p.getBrand() + " " + p.getModelName() + " has excess stock (" + curStock + " units). Holding cost risk."
                        ));
                    }

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("productId", pid);
                    summary.put("brand", p.getBrand());
                    summary.put("modelName", p.getModelName());
                    summary.put("price", p.getPrice());
                    summary.put("currentStock", curStock);
                    summary.put("monthlySales", curSales);
                    summary.put("predictedDemand", demandResult.getPredictedDemand());
                    summary.put("visibilityScore", vis);
                    summary.put("accuracyScore", acc);
                    summary.put("engagementScore", eng);
                    summary.put("classification", className);
                    summary.put("reorderStatus", rec.getStatus());
                    summary.put("recommendedReorder", rec.getRecommendedReorderQty());
                    productSummaries.add(summary);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("totalModels", totalModels);
                response.put("totalUnitsSold", totalUnitsSold);
                response.put("totalStock", totalStock);
                response.put("reordersRequired", reordersRequired);
                response.put("avgVisibility", totalModels > 0 ? Math.round((totalVis / totalModels) * 10.0) / 10.0 : 0.0);
                response.put("avgAccuracy", totalModels > 0 ? Math.round((totalAcc / totalModels) * 10.0) / 10.0 : 0.0);
                response.put("avgEngagement", totalModels > 0 ? Math.round((totalEng / totalModels) * 10.0) / 10.0 : 0.0);
                response.put("alerts", alerts);
                response.put("brandSales", brandSales);
                response.put("classificationCounts", classificationCounts);
                response.put("products", productSummaries);

                sendJsonResponse(exchange, 200, Map.of("success", true, "mainDashboard", response));

            } catch (Exception e) {
                LOGGER.severe("Main dashboard error: " + e.getMessage());
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class AddProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);

                String brand = json.get("brand").getAsString();
                String modelName = json.get("modelName").getAsString();
                String category = json.has("category") ? json.get("category").getAsString() : "Smartphone";
                double price = json.get("price").getAsDouble();
                String ram = json.has("ram") ? json.get("ram").getAsString() : "8GB";
                String storage = json.has("storage") ? json.get("storage").getAsString() : "128GB";
                String processor = json.has("processor") ? json.get("processor").getAsString() : "Octa-Core";
                String battery = json.has("battery") ? json.get("battery").getAsString() : "5000 mAh";
                String display = json.has("display") ? json.get("display").getAsString() : "6.6\" AMOLED";
                String camera = json.has("camera") ? json.get("camera").getAsString() : "50MP Main";

                MobileProduct p = new MobileProduct(0, brand, modelName, category, price, LocalDate.now(),
                        ram, storage, processor, battery, display, camera, "Android 15", "5G");

                int pid = productDAO.addProduct(p);
                p.setProductId(pid);

                // Optional sales, inventory, and AI data if provided
                int currentStock = json.has("currentStock") ? json.get("currentStock").getAsInt() : 50;
                int safetyStock = json.has("safetyStock") ? json.get("safetyStock").getAsInt() : 20;
                int leadTime = json.has("leadTime") ? json.get("leadTime").getAsInt() : 7;
                int monthlySales = json.has("monthlySales") ? json.get("monthlySales").getAsInt() : 60;
                int prevSales = json.has("prevMonthlySales") ? json.get("prevMonthlySales").getAsInt() : 50;

                int calcReorder = (int) Math.ceil((monthlySales / 30.0 * leadTime) + safetyStock);
                InventoryStatus status = currentStock <= safetyStock ? InventoryStatus.REORDER_REQUIRED :
                        (currentStock <= calcReorder ? InventoryStatus.LOW_STOCK : InventoryStatus.IN_STOCK);

                Inventory inv = new Inventory(0, pid, currentStock, safetyStock, calcReorder, leadTime, currentStock, status);
                inventoryDAO.addOrUpdateInventory(inv);

                double growth = prevSales > 0 ? ((double)(monthlySales - prevSales) / prevSales * 100.0) : 0.0;
                Sales s = new Sales(0, pid, LocalDate.now(), monthlySales * 5, (int)(monthlySales/4.0),
                        monthlySales, prevSales, monthlySales * price, growth, growth > 10 ? "Upward" : "Stable", (int)(monthlySales * 1.15));
                salesDAO.addOrUpdateSales(s);

                int totalQueries = json.has("totalQueries") ? json.get("totalQueries").getAsInt() : 1000;
                int appearances = json.has("appearances") ? json.get("appearances").getAsInt() : 750;
                AISearchData ai = new AISearchData(0, pid, totalQueries, appearances, 600, 200, 5, 5, 350, 180, 120, 190, 80, 75);
                ai.addClaimComparison(new AISearchData.ClaimComparison("Battery", battery, battery, true, "Verified claim"));
                ai.setVisibilityScore(Math.round(((double)appearances / totalQueries * 100.0) * 10.0) / 10.0);
                ai.setAccuracyScore(100.0);
                ai.setEngagementScore(78.0);
                aiDAO.addOrUpdateAISearchData(ai);

                // Run automatic calculation
                DemandPredictionResult demandResult = demandPrediction.predictDemand(s, inv, ai);
                ProductClassifier.ClassificationOutput classification = productClassifier.classifyProduct(s, ai);
                ReorderRecommendation rec = recommendationEngine.generateRecommendation(s, inv, ai, demandResult, classification);

                analyticsDAO.saveAnalytics(pid, ai.getVisibilityScore(), ai.getAccuracyScore(), ai.getEngagementScore(),
                        demandResult.getPredictedDemand(), demandResult.getDemandGrowthPct(), demandResult.getDemandTrend(),
                        classification.getClassification().getLabel());
                recommendationDAO.saveRecommendation(rec);

                sendJsonResponse(exchange, 200, Map.of("success", true, "productId", pid, "message", "Product added and auto-analyzed successfully."));

            } catch (InvalidProductException e) {
                sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("success", false, "error", e.getMessage()));
            }
        }
    }

    private class CsvUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                InputStream in = exchange.getRequestBody();
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                // Extract CSV text whether sent as raw CSV or multipart payload
                String csvText = extractCsvContent(body);
                ByteArrayInputStream bais = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

                CSVDataImporter importer = new CSVDataImporter();
                CSVDataImporter.ImportResult result = importer.importCSVStream(bais);

                lastImportErrorsCsv = result.generateErrorsCsv();

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("totalRowsDetected", result.getTotalRowsDetected());
                resp.put("productsImported", result.getProductsImported());
                resp.put("rowsSkipped", result.getRowsSkipped());
                resp.put("errorsEncountered", result.getErrorsEncountered());
                resp.put("messages", result.getMessages());
                resp.put("errorDetails", result.getErrorDetails());
                resp.put("errorsCsv", lastImportErrorsCsv);

                sendJsonResponse(exchange, 200, resp);

            } catch (Exception e) {
                LOGGER.severe("CSV upload error: " + e.getMessage());
                sendJsonResponse(exchange, 400, Map.of("success", false, "error", "Failed to process CSV: " + e.getMessage()));
            }
        }

        private String extractCsvContent(String raw) {
            if (raw == null) return "";
            if (raw.contains("------WebKitFormBoundary") || raw.contains("Content-Disposition:")) {
                // Parse multipart boundary
                int headerEnd = raw.indexOf("\r\n\r\n");
                if (headerEnd != -1) {
                    String afterHeader = raw.substring(headerEnd + 4);
                    int boundaryEnd = afterHeader.lastIndexOf("\r\n------");
                    if (boundaryEnd != -1) {
                        return afterHeader.substring(0, boundaryEnd);
                    }
                    return afterHeader;
                }
            }
            return raw;
        }
    }

    private class CsvErrorsDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "text/csv; charset=UTF-8");
                headers.set("Content-Disposition", "attachment; filename=\"csv_validation_errors.csv\"");

                String csv = (lastImportErrorsCsv != null && !lastImportErrorsCsv.isEmpty()) ?
                        lastImportErrorsCsv :
                        "Row Number,Column,Actual Value,Failure Reason,Suggested Correction,Original Row Data\n";

                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error generating error report: " + e.getMessage());
            }
        }
    }

    private class CsvExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                CSVDataExporter exporter = new CSVDataExporter();
                String csv = exporter.generateCSVString();

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "text/csv; charset=UTF-8");
                headers.set("Content-Disposition", "attachment; filename=\"mobile_inventory_analytics.csv\"");

                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error generating CSV export: " + e.getMessage());
            }
        }
    }

    private class CsvSampleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sample = "brand,model_name,price,ram,storage,processor,battery,sales,current_stock,lead_time\n" +
                    "Samsung,Galaxy S24 Ultra,129999,12,256,Snapdragon 8 Gen 3,5000,500,80,7\n" +
                    "Samsung,Galaxy A56,39999,8,256,Exynos 1580,5000,350,120,5\n" +
                    "Apple,iPhone 15,69999,6,128,A16 Bionic,3349,450,60,7\n" +
                    "OnePlus,OnePlus 12,64999,16,512,Snapdragon 8 Gen 3,5400,420,75,7\n" +
                    "Xiaomi,Redmi Note 14 Pro+,29999,12,256,Snapdragon 7s Gen 3,6200,380,95,5\n" +
                    "Google Pixel,Pixel 9,79999,12,128,Google Tensor G4,4700,290,50,7\n" +
                    "Vivo,Vivo V40 Pro,49999,12,512,MediaTek Dimensity 9200+,5500,310,65,7\n" +
                    "Oppo,Find X8 Pro,89999,16,512,Dimensity 9400,5910,240,40,7\n";

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/csv; charset=UTF-8");
            headers.set("Content-Disposition", "attachment; filename=\"sample_mobile_import.csv\"");

            byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class ReportDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                ReportGenerator generator = new ReportGenerator();
                String report = generator.generateMarkdownReport();

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "text/markdown; charset=UTF-8");
                headers.set("Content-Disposition", "attachment; filename=\"AI_Mobile_SupplyChain_Audit_Report.md\"");

                byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error generating report: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // STATIC FILE SERVER
    // ==========================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Remove leading slash
            String resourcePath = "web" + path;
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

            // Also check disk if running in development mode
            if (in == null) {
                File localFile = new File("src/main/resources/" + resourcePath);
                if (localFile.exists()) {
                    in = new FileInputStream(localFile);
                }
            }

            if (in == null) {
                // If not found and not an API call, fallback to index.html for SPA
                if (!path.startsWith("/api/")) {
                    InputStream indexIn = getClass().getClassLoader().getResourceAsStream("web/index.html");
                    if (indexIn == null) {
                        File localIndex = new File("src/main/resources/web/index.html");
                        if (localIndex.exists()) {
                            indexIn = new FileInputStream(localIndex);
                        }
                    }
                    if (indexIn != null) {
                        serveStream(exchange, indexIn, "text/html; charset=UTF-8");
                        return;
                    }
                }
                String notFound = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = getMimeType(path);
            serveStream(exchange, in, contentType);
        }

        private void serveStream(HttpExchange exchange, InputStream in, String mimeType) throws IOException {
            try (in) {
                byte[] bytes = in.readAllBytes();
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", mimeType);
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    private void sendJsonResponse(HttpExchange exchange, int status, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(k, v);
        }
        return params;
    }
}
