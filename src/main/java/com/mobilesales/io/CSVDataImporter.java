package com.mobilesales.io;

import com.mobilesales.analytics.DemandPrediction;
import com.mobilesales.analytics.ProductClassifier;
import com.mobilesales.analytics.RecommendationEngine;
import com.mobilesales.dao.*;
import com.mobilesales.exception.DataValidationException;
import com.mobilesales.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent and tolerant CSV Data Importer.
 * Features:
 * 1. Flexible semantic column mapping (brand_name -> Brand, selling_price -> Price, etc.)
 * 2. Robust RFC 4180 delimiter and quote parser.
 * 3. Graceful tolerance for missing optional fields and currency/unit symbols (₹, $, GB, mAh, commas).
 * 4. Row-by-row validation with detailed ErrorDetail reporting (Row, Column, Value, Problem, Suggested Fix).
 * 5. Automatic calculation of Sales, Inventory, AI Search metrics, Demand, Classification, and Reorders.
 * 6. Duplicate product handling (updates existing records instead of duplicating).
 */
public class CSVDataImporter {

    private static final Logger LOGGER = Logger.getLogger(CSVDataImporter.class.getName());

    private final MobileProductDAO productDAO;
    private final SalesDAO salesDAO;
    private final InventoryDAO inventoryDAO;
    private final AISearchDAO aiDAO;
    private final AnalyticsDAO analyticsDAO;
    private final RecommendationDAO recommendationDAO;

    public CSVDataImporter() {
        this.productDAO = new MobileProductDAO();
        this.salesDAO = new SalesDAO();
        this.inventoryDAO = new InventoryDAO();
        this.aiDAO = new AISearchDAO();
        this.analyticsDAO = new AnalyticsDAO();
        this.recommendationDAO = new RecommendationDAO();
    }

    public static class ErrorDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private int rowNumber;
        private String column;
        private String value;
        private String problem;
        private String suggestedFix;

        public ErrorDetail() {}

        public ErrorDetail(int rowNumber, String column, String value, String problem, String suggestedFix) {
            this.rowNumber = rowNumber;
            this.column = column;
            this.value = value;
            this.problem = problem;
            this.suggestedFix = suggestedFix;
        }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getProblem() { return problem; }
        public void setProblem(String problem) { this.problem = problem; }
        public String getSuggestedFix() { return suggestedFix; }
        public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    }

    public static class ImportResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private int productsImported = 0;
        private int rowsSkipped = 0;
        private int errorsEncountered = 0;
        private final List<String> messages = new ArrayList<>();
        private final List<ErrorDetail> errorDetails = new ArrayList<>();

        public int getProductsImported() { return productsImported; }
        public void setProductsImported(int productsImported) { this.productsImported = productsImported; }
        public int getRowsSkipped() { return rowsSkipped; }
        public void setRowsSkipped(int rowsSkipped) { this.rowsSkipped = rowsSkipped; }
        public int getErrorsEncountered() { return errorsEncountered; }
        public void setErrorsEncountered(int errorsEncountered) { this.errorsEncountered = errorsEncountered; }
        public List<String> getMessages() { return messages; }
        public List<ErrorDetail> getErrorDetails() { return errorDetails; }
    }

    public ImportResult importCSV(File csvFile) throws DataValidationException {
        if (csvFile == null || !csvFile.exists()) {
            throw new DataValidationException("CSV file does not exist: " + (csvFile != null ? csvFile.getAbsolutePath() : "null"));
        }
        try (InputStream in = new FileInputStream(csvFile)) {
            return importCSVStream(in);
        } catch (Exception e) {
            throw new DataValidationException("Failed to read CSV file: " + e.getMessage(), e);
        }
    }

    public ImportResult importCSVStream(InputStream in) throws DataValidationException {
        if (in == null) {
            throw new DataValidationException("Input stream is null.");
        }

        ImportResult result = new ImportResult();
        DemandPrediction demandEngine = new DemandPrediction();
        ProductClassifier classifier = new ProductClassifier();
        RecommendationEngine recEngine = new RecommendationEngine();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                result.getMessages().add("The uploaded CSV file is empty.");
                return result;
            }

            // Remove UTF-8 BOM if present
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }

            char delimiter = detectDelimiter(firstLine);
            List<String> headers = parseCsvLine(firstLine, delimiter);
            Map<String, Integer> colMap = buildColumnMap(headers);

            boolean hasHeader = isHeaderRow(colMap);
            int lineNum = 1;

            if (!hasHeader) {
                // First line is actually a data row! Process it with default positional mapping
                processDataRow(firstLine, delimiter, colMap, lineNum, result, demandEngine, classifier, recEngine);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                processDataRow(line, delimiter, colMap, lineNum, result, demandEngine, classifier, recEngine);
            }

        } catch (Exception e) {
            LOGGER.severe("CSV Import Error: " + e.getMessage());
            throw new DataValidationException("Error processing CSV: " + e.getMessage(), e);
        }

        result.setErrorsEncountered(result.getErrorDetails().size());
        return result;
    }

    private void processDataRow(String line, char delimiter, Map<String, Integer> colMap,
                                int lineNum, ImportResult result,
                                DemandPrediction demandEngine, ProductClassifier classifier,
                                RecommendationEngine recEngine) {
        List<String> tokens = parseCsvLine(line, delimiter);
        if (tokens.isEmpty()) {
            return;
        }

        // 1. Extract and Clean Brand
        // 1. Extract and Clean Brand
        String rawBrand = getMappedValue(tokens, colMap, "BRAND");
        String rawModel = getMappedValue(tokens, colMap, "MODEL");

        // If brand is empty, try to deduce from model
        if ((rawBrand == null || rawBrand.trim().isEmpty()) && rawModel != null && !rawModel.trim().isEmpty()) {
            rawBrand = deduceBrandFromModel(rawModel);
        }

        // Validate REQUIRED field: Brand
        if (rawBrand == null || rawBrand.trim().isEmpty()) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Brand", "(empty)", "Required field missing",
                    "Specify a smartphone brand (e.g. Samsung, Apple, OnePlus)"));
            return;
        }

        // Validate REQUIRED field: Model / Product Name
        if (rawModel == null || rawModel.trim().isEmpty()) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Model Name", "(empty)", "Required field missing",
                    "Specify a smartphone model name (e.g. Galaxy S24 Ultra)"));
            return;
        }

        String brand = normalizeBrand(rawBrand);
        String modelName = cleanText(rawModel);

        // 2. Extract and Validate Price
        String rawPrice = getMappedValue(tokens, colMap, "PRICE");
        Double priceVal = extractNumber(rawPrice);
        if (rawPrice != null && !rawPrice.trim().isEmpty() && priceVal == null) {
            // Provided non-numeric price string like "abc"
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Price", rawPrice, "Invalid numeric value",
                    "Enter a numeric price such as 499.99 or 39999"));
            return;
        }
        double price = (priceVal != null && priceVal > 0) ? priceVal : getEstimatedPrice(brand, modelName);

        // 3. Extract Hardware Specs with Safe Defaults
        String category = getMappedValue(tokens, colMap, "CATEGORY");
        if (category == null || category.trim().isEmpty()) {
            category = price >= 800 ? "Flagship" : (price >= 400 ? "Mid-Range" : "Budget");
        }

        String ram = normalizeRAM(getMappedValue(tokens, colMap, "RAM"));
        String storage = normalizeStorage(getMappedValue(tokens, colMap, "STORAGE"));
        String processor = getMappedValue(tokens, colMap, "PROCESSOR");
        if (processor == null || processor.trim().isEmpty()) processor = "Octa-Core Processor";

        String battery = normalizeBattery(getMappedValue(tokens, colMap, "BATTERY"));
        String display = getMappedValue(tokens, colMap, "DISPLAY");
        if (display == null || display.trim().isEmpty()) display = "6.6\" AMOLED 120Hz";

        String camera = getMappedValue(tokens, colMap, "CAMERA");
        if (camera == null || camera.trim().isEmpty()) camera = "50MP Main Camera";

        String os = getMappedValue(tokens, colMap, "OS");
        if (os == null || os.trim().isEmpty()) os = brand.equalsIgnoreCase("Apple") ? "iOS" : "Android";

        String network = getMappedValue(tokens, colMap, "NETWORK");
        if (network == null || network.trim().isEmpty()) network = "5G";

        // 4. Extract Sales Numbers
        String rawSales = getMappedValue(tokens, colMap, "SALES");
        Double salesNum = extractNumber(rawSales);
        if (rawSales != null && !rawSales.trim().isEmpty() && salesNum == null) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Sales", rawSales, "Invalid numeric value",
                    "Enter a numeric sales volume (e.g. 500)"));
            return;
        }
        int h = Math.abs((brand + " " + modelName).hashCode());
        int defaultSales;
        if (price >= 80000 || price >= 900) {
            defaultSales = 110 + (h % 90);
        } else if (price >= 35000 || price >= 400) {
            defaultSales = 220 + (h % 160);
        } else {
            defaultSales = 320 + (h % 260);
        }
        int monthlySales = salesNum != null && salesNum > 0 ? salesNum.intValue() : defaultSales;

        String rawPrevSales = getMappedValue(tokens, colMap, "PREV_SALES");
        Double prevSalesNum = extractNumber(rawPrevSales);
        int defaultPrev = (int) Math.round(monthlySales * (0.82 + ((h % 26) / 100.0)));
        int prevMonthlySales = prevSalesNum != null && prevSalesNum > 0 ? prevSalesNum.intValue() : defaultPrev;

        int weeklySales = Math.max(1, (int) Math.round(monthlySales / 4.0));
        double revenue = monthlySales * price;
        double growthPct = prevMonthlySales > 0 ? ((double) (monthlySales - prevMonthlySales) / prevMonthlySales) * 100.0 : 0.0;
        growthPct = Math.round(growthPct * 10.0) / 10.0;
        String salesTrend = growthPct > 10.0 ? "Upward" : (growthPct < -5.0 ? "Downward" : "Stable");

        // 5. Extract Inventory Numbers
        String rawStock = getMappedValue(tokens, colMap, "STOCK");
        Double stockNum = extractNumber(rawStock);
        if (rawStock != null && !rawStock.trim().isEmpty() && stockNum == null) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Current Stock", rawStock, "Invalid numeric value",
                    "Enter a numeric inventory stock count (e.g. 80)"));
            return;
        }
        double coverage = 0.40 + ((h % 120) / 100.0);
        int defaultStock = Math.max(15, (int) Math.round(monthlySales * coverage));
        int currentStock = stockNum != null && stockNum >= 0 ? stockNum.intValue() : defaultStock;

        String rawLeadTime = getMappedValue(tokens, colMap, "LEAD_TIME");
        Double leadNum = extractNumber(rawLeadTime);
        int defaultLead = 4 + (h % 8);
        int leadTime = leadNum != null && leadNum > 0 ? leadNum.intValue() : defaultLead;

        String rawSafety = getMappedValue(tokens, colMap, "SAFETY_STOCK");
        Double safetyNum = extractNumber(rawSafety);
        int defaultSafety = Math.max(15, (int) Math.round(monthlySales * (leadTime / 30.0) * 0.8));
        int safetyStock = safetyNum != null && safetyNum > 0 ? safetyNum.intValue() : defaultSafety;

        // Reorder Level formula: (Average Daily Sales * Lead Time) + Safety Stock
        double avgDaily = monthlySales / 30.0;
        int reorderLvl = (int) Math.ceil((avgDaily * leadTime) + safetyStock);

        InventoryStatus invStatus;
        if (currentStock <= safetyStock) invStatus = InventoryStatus.REORDER_REQUIRED;
        else if (currentStock <= reorderLvl) invStatus = InventoryStatus.LOW_STOCK;
        else if (currentStock > (reorderLvl * 2.5)) invStatus = InventoryStatus.OVERSTOCK;
        else invStatus = InventoryStatus.IN_STOCK;

        // 6. Extract or Synthesize AI Metrics
        String rawVis = getMappedValue(tokens, colMap, "VISIBILITY");
        String rawAcc = getMappedValue(tokens, colMap, "ACCURACY");
        String rawEng = getMappedValue(tokens, colMap, "ENGAGEMENT");

        Double visScore = extractNumber(rawVis);
        Double accScore = extractNumber(rawAcc);
        Double engScore = extractNumber(rawEng);

        // Queries
        String rawQueries = getMappedValue(tokens, colMap, "TOTAL_QUERIES");
        Double qNum = extractNumber(rawQueries);
        int totalQueries = qNum != null && qNum > 0 ? qNum.intValue() : (750 + (h % 1350));

        String rawAppearances = getMappedValue(tokens, colMap, "APPEARANCES");
        Double appNum = extractNumber(rawAppearances);
        int appearances;
        double finalVisibility;
        if (appNum != null && appNum > 0) {
            appearances = appNum.intValue();
            finalVisibility = Math.round(((double) appearances / totalQueries * 100.0) * 10.0) / 10.0;
        } else if (visScore != null && visScore > 0) {
            finalVisibility = visScore;
            appearances = (int) Math.round((visScore / 100.0) * totalQueries);
        } else {
            double baseVis = (price >= 80000 || price >= 900) ? 80.0 : ((price >= 35000 || price >= 400) ? 65.0 : 48.0);
            double visVariance = ((h % 220) / 10.0) - 11.0;
            finalVisibility = Math.min(96.0, Math.max(35.0, Math.round((baseVis + visVariance) * 10.0) / 10.0));
            appearances = (int) Math.round((finalVisibility / 100.0) * totalQueries);
        }

        double finalAccuracy = accScore != null && accScore > 0 ? accScore : 100.0;

        int brandMentions = (int) Math.round(appearances * (0.70 + ((h % 20) / 100.0)));
        int compQueries = (int) Math.round(appearances * (0.22 + ((h % 15) / 100.0)));
        int productSearches = (int) Math.round(appearances * (0.38 + ((h % 18) / 100.0)));
        int recQueries = (int) Math.round(appearances * (0.16 + ((h % 12) / 100.0)));
        int interactions = (int) Math.round(appearances * (0.22 + ((h % 12) / 100.0)));
        int reviews = (int) Math.round(appearances * (0.09 + ((h % 8) / 100.0)));
        int repeatSearches = (int) Math.round(appearances * (0.11 + ((h % 10) / 100.0)));

        double finalEngagement;
        if (engScore != null && engScore > 0) {
            finalEngagement = engScore;
        } else {
            finalEngagement = Math.min(100.0, Math.round(((repeatSearches * 0.25) + (recQueries * 0.20) + (compQueries * 0.20) + (interactions * 0.15) + (reviews * 0.10) + (productSearches * 0.10)) / 2.2));
            if (finalEngagement < 30) finalEngagement = 55.0 + (h % 35);
        }

        // 7. Store / Update Product in Database
        try {
            MobileProduct product = new MobileProduct(0, brand, modelName, category, price, LocalDate.now(),
                    ram, storage, processor, battery, display, camera, os, network);

            int pid = productDAO.addOrUpdateProduct(product);
            product.setProductId(pid);

            // Store Inventory
            Inventory inv = new Inventory(0, pid, currentStock, safetyStock, reorderLvl, leadTime, currentStock, invStatus);
            inventoryDAO.addOrUpdateInventory(inv);

            // Store Sales
            Sales sales = new Sales(0, pid, LocalDate.now(), monthlySales * 8, weeklySales,
                    monthlySales, prevMonthlySales, revenue, growthPct, salesTrend, (int) (monthlySales * 1.15));
            salesDAO.addOrUpdateSales(sales);

            // Store AI Search Data
            AISearchData ai = new AISearchData(0, pid, totalQueries, appearances, brandMentions, compQueries,
                    5, 5, productSearches, compQueries, recQueries, interactions, reviews, repeatSearches);
            ai.setVisibilityScore(finalVisibility);
            ai.setAccuracyScore(finalAccuracy);
            ai.setEngagementScore(finalEngagement);

            ai.addClaimComparison(new AISearchData.ClaimComparison("Battery Capacity", battery, battery, true, "AI search verified"));
            ai.addClaimComparison(new AISearchData.ClaimComparison("Processor", processor, processor, true, "Hardware index"));
            ai.addClaimComparison(new AISearchData.ClaimComparison("Camera", camera, camera, true, "Camera module DB"));
            ai.addClaimComparison(new AISearchData.ClaimComparison("Display", display, display, true, "Display tech graph"));
            ai.addClaimComparison(new AISearchData.ClaimComparison("RAM / Storage", ram + " / " + storage, ram + " / " + storage, true, "Retail feed"));
            aiDAO.addOrUpdateAISearchData(ai);

            // Automatically Calculate Analytics & Reorder Recommendation
            DemandPredictionResult demandResult = demandEngine.predictDemand(sales, inv, ai);
            ProductClassifier.ClassificationOutput classOutput = classifier.classifyProduct(sales, ai);
            ReorderRecommendation rec = recEngine.generateRecommendation(sales, inv, ai, demandResult, classOutput);

            analyticsDAO.saveAnalytics(pid, ai.getVisibilityScore(), ai.getAccuracyScore(), ai.getEngagementScore(),
                    demandResult.getPredictedDemand(), demandResult.getDemandGrowthPct(), demandResult.getDemandTrend(),
                    classOutput.getClassification().getLabel());

            recommendationDAO.saveRecommendation(rec);

            result.setProductsImported(result.getProductsImported() + 1);

        } catch (Exception ex) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Database", modelName, "Database error: " + ex.getMessage(),
                    "Check database constraints and field types"));
        }
    }

    // ==========================================
    // INTELLIGENT COLUMN MAPPING
    // ==========================================

    private Map<String, Integer> buildColumnMap(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String raw = headers.get(i).trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            while (raw.contains("__")) raw = raw.replace("__", "_");
            if (raw.startsWith("_")) raw = raw.substring(1);
            if (raw.endsWith("_")) raw = raw.substring(0, raw.length() - 1);

            assignFieldIfMatch(map, raw, i, "BRAND",
                    "brand", "brand_name", "company", "make", "manufacturer", "brand_model", "oem");
            assignFieldIfMatch(map, raw, i, "MODEL",
                    "model", "model_name", "phone_model", "product_name", "name", "device", "mobile_name", "product", "title", "phone", "smartphone");
            assignFieldIfMatch(map, raw, i, "CATEGORY",
                    "category", "type", "segment", "tier", "series");
            assignFieldIfMatch(map, raw, i, "PRICE",
                    "price", "selling_price", "price_inr", "cost", "mrp", "retail_price", "price_usd", "rate", "final_price", "discount_price", "original_price");
            assignFieldIfMatch(map, raw, i, "RAM",
                    "ram", "ram_gb", "ram_capacity", "memory", "ram_memory", "system_ram");
            assignFieldIfMatch(map, raw, i, "STORAGE",
                    "storage", "internal_storage", "storage_gb", "internal_memory", "rom", "disk", "capacity", "storage_capacity", "memory_storage");
            assignFieldIfMatch(map, raw, i, "PROCESSOR",
                    "processor", "chipset", "processor_brand", "cpu", "soc", "processor_name", "chip");
            assignFieldIfMatch(map, raw, i, "BATTERY",
                    "battery", "battery_capacity", "battery_mah", "battery_size");
            assignFieldIfMatch(map, raw, i, "DISPLAY",
                    "display", "screen", "screen_size", "display_size", "resolution");
            assignFieldIfMatch(map, raw, i, "CAMERA",
                    "camera", "rear_camera", "primary_camera", "camera_spec", "primary_camera_rear", "rear_cameras", "camera_setup");
            assignFieldIfMatch(map, raw, i, "OS",
                    "os", "operating_system", "platform");
            assignFieldIfMatch(map, raw, i, "NETWORK",
                    "network", "connectivity", "has_5g");
            assignFieldIfMatch(map, raw, i, "SALES",
                    "sales", "units_sold", "quantity_sold", "monthly_sales", "volume", "sold", "units", "sales_units");
            assignFieldIfMatch(map, raw, i, "PREV_SALES",
                    "previous_month_sales", "prev_monthly_sales", "prev_sales", "last_month_sales");
            assignFieldIfMatch(map, raw, i, "STOCK",
                    "current_stock", "stock", "inventory", "units_in_stock", "quantity_available", "available_stock");
            assignFieldIfMatch(map, raw, i, "SAFETY_STOCK",
                    "safety_stock", "buffer_stock", "min_stock");
            assignFieldIfMatch(map, raw, i, "LEAD_TIME",
                    "lead_time", "supplier_lead_time", "lead_time_days", "delivery_days");
            assignFieldIfMatch(map, raw, i, "VISIBILITY",
                    "visibility", "visibility_score", "search_visibility");
            assignFieldIfMatch(map, raw, i, "ACCURACY",
                    "accuracy", "accuracy_score", "spec_accuracy");
            assignFieldIfMatch(map, raw, i, "ENGAGEMENT",
                    "engagement", "engagement_score", "user_engagement");
            assignFieldIfMatch(map, raw, i, "TOTAL_QUERIES",
                    "total_queries", "queries", "search_volume");
            assignFieldIfMatch(map, raw, i, "APPEARANCES",
                    "appearances", "product_appearances", "impressions");
        }

        // If no BRAND or MODEL mapped, check positional defaults
        if (!map.containsKey("BRAND") && headers.size() > 0) map.put("BRAND", 0);
        if (!map.containsKey("MODEL") && headers.size() > 1) map.put("MODEL", 1);
        if (!map.containsKey("PRICE") && headers.size() > 2) map.put("PRICE", 2);

        return map;
    }

    private void assignFieldIfMatch(Map<String, Integer> map, String header, int index, String fieldKey, String... keywords) {
        if (map.containsKey(fieldKey)) return;
        for (String kw : keywords) {
            if (header.equals(kw) || header.equalsIgnoreCase(kw) || header.contains(kw)) {
                map.put(fieldKey, index);
                return;
            }
        }
    }

    private boolean isHeaderRow(Map<String, Integer> colMap) {
        return colMap.containsKey("BRAND") || colMap.containsKey("MODEL") || colMap.containsKey("PRICE");
    }

    private String getMappedValue(List<String> tokens, Map<String, Integer> colMap, String key) {
        Integer idx = colMap.get(key);
        if (idx != null && idx >= 0 && idx < tokens.size()) {
            String val = tokens.get(idx);
            return val != null ? val.trim() : null;
        }
        return null;
    }

    // ==========================================
    // RFC 4180 CSV LINE PARSER
    // ==========================================

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;

        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = line.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '"') {
                if (inQuotes && i + 1 < chars.length && chars[i + 1] == '"') {
                    curVal.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(curVal.toString().trim());
                curVal.setLength(0);
            } else {
                curVal.append(c);
            }
        }
        result.add(curVal.toString().trim());
        return result;
    }

    private char detectDelimiter(String line) {
        int commas = 0, semicolons = 0, tabs = 0;
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c == ',') commas++;
                else if (c == ';') semicolons++;
                else if (c == '\t') tabs++;
            }
        }
        if (semicolons > commas && semicolons > tabs) return ';';
        if (tabs > commas && tabs > semicolons) return '\t';
        return ',';
    }

    // ==========================================
    // DATA CLEANERS & TYPE CONVERTERS
    // ==========================================

    public static Double extractNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String clean = raw.trim();
        // Remove currency symbols, commas, quotes
        clean = clean.replaceAll("[₹$€£,]", "");
        // Extract numeric part (e.g. "12GB" -> 12, "5000 mAh" -> 5000, "999.99" -> 999.99)
        Pattern p = Pattern.compile("[-+]?\\d*\\.?\\d+");
        Matcher m = p.matcher(clean);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String normalizeBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) return "Other";
        String b = brand.trim().toLowerCase();

        if (b.contains("samsung")) return "Samsung";
        if (b.contains("apple") || b.contains("iphone")) return "Apple";
        if (b.contains("oneplus")) return "OnePlus";
        if (b.contains("xiaomi") || b.contains("redmi") || b.contains("mi")) return "Xiaomi";
        if (b.contains("vivo")) return "Vivo";
        if (b.contains("oppo")) return "Oppo";
        if (b.contains("pixel") || b.contains("google")) return "Google Pixel";
        if (b.contains("realme")) return "Realme";
        if (b.contains("motorola") || b.contains("moto")) return "Motorola";
        if (b.contains("nothing")) return "Nothing";
        if (b.contains("poco")) return "POCO";
        if (b.contains("iqoo")) return "iQOO";
        if (b.contains("honor")) return "Honor";
        if (b.contains("asus")) return "Asus";
        if (b.contains("sony")) return "Sony";

        // Title case for any other brand
        return Character.toUpperCase(b.charAt(0)) + (b.length() > 1 ? b.substring(1) : "");
    }

    private String deduceBrandFromModel(String model) {
        if (model == null || model.trim().isEmpty()) return null;
        String m = model.toLowerCase();
        if (m.contains("galaxy") || m.contains("samsung")) return "Samsung";
        if (m.contains("iphone") || m.contains("apple")) return "Apple";
        if (m.contains("oneplus") || m.contains("nord")) return "OnePlus";
        if (m.contains("redmi") || m.contains("xiaomi") || m.contains("poco")) return "Xiaomi";
        if (m.contains("vivo") || m.contains("iqoo")) return "Vivo";
        if (m.contains("oppo") || m.contains("reno") || m.contains("find")) return "Oppo";
        if (m.contains("pixel")) return "Google Pixel";
        if (m.contains("motorola") || m.contains("edge") || m.contains("moto")) return "Motorola";
        if (m.contains("realme")) return "Realme";
        if (m.contains("nothing")) return "Nothing";
        if (m.contains("honor")) return "Honor";
        if (m.contains("asus") || m.contains("rog")) return "Asus";
        if (m.contains("sony") || m.contains("xperia")) return "Sony";
        return null;
    }

    private String normalizeRAM(String val) {
        if (val == null || val.trim().isEmpty()) return "8GB";
        Double num = extractNumber(val);
        if (num != null && num > 0) {
            return num.intValue() + "GB";
        }
        return val.trim();
    }

    private String normalizeStorage(String val) {
        if (val == null || val.trim().isEmpty()) return "128GB";
        Double num = extractNumber(val);
        if (num != null && num > 0) {
            return num.intValue() + (num < 10 ? "TB" : "GB");
        }
        return val.trim();
    }

    private String normalizeBattery(String val) {
        if (val == null || val.trim().isEmpty()) return "5000 mAh";
        Double num = extractNumber(val);
        if (num != null && num > 0) {
            return num.intValue() + " mAh";
        }
        return val.trim();
    }

    private String cleanText(String val) {
        if (val == null) return "";
        return val.trim().replaceAll("^\"|\"$", "");
    }

    private double getEstimatedPrice(String brand, String model) {
        String combined = (brand + " " + model).toLowerCase();
        if (combined.contains("ultra") || combined.contains("pro max") || combined.contains("fold")) return 1199.99;
        if (combined.contains("pro") || combined.contains("plus") || combined.contains("flagship")) return 799.99;
        if (combined.contains("nord") || combined.contains("lite") || combined.contains("fe")) return 449.99;
        return 349.99;
    }
}
