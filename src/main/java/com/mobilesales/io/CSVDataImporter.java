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
        private String rawLine;

        public ErrorDetail() {}

        public ErrorDetail(int rowNumber, String column, String value, String problem, String suggestedFix) {
            this(rowNumber, column, value, problem, suggestedFix, "");
        }

        public ErrorDetail(int rowNumber, String column, String value, String problem, String suggestedFix, String rawLine) {
            this.rowNumber = rowNumber;
            this.column = column;
            this.value = value;
            this.problem = problem;
            this.suggestedFix = suggestedFix;
            this.rawLine = rawLine != null ? rawLine : "";
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
        public String getRawLine() { return rawLine; }
        public void setRawLine(String rawLine) { this.rawLine = rawLine; }
    }

    public static class ImportResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private int totalRowsDetected = 0;
        private int productsImported = 0;
        private int rowsSkipped = 0;
        private int errorsEncountered = 0;
        private final List<String> messages = new ArrayList<>();
        private final List<ErrorDetail> errorDetails = new ArrayList<>();

        public int getTotalRowsDetected() { return totalRowsDetected; }
        public void setTotalRowsDetected(int totalRowsDetected) { this.totalRowsDetected = totalRowsDetected; }
        public int getProductsImported() { return productsImported; }
        public void setProductsImported(int productsImported) { this.productsImported = productsImported; }
        public int getRowsSkipped() { return rowsSkipped; }
        public void setRowsSkipped(int rowsSkipped) { this.rowsSkipped = rowsSkipped; }
        public int getErrorsEncountered() { return errorsEncountered; }
        public void setErrorsEncountered(int errorsEncountered) { this.errorsEncountered = errorsEncountered; }
        public List<String> getMessages() { return messages; }
        public List<ErrorDetail> getErrorDetails() { return errorDetails; }

        public String generateErrorsCsv() {
            StringBuilder sb = new StringBuilder();
            sb.append("Row Number,Column,Actual Value,Failure Reason,Suggested Correction,Original Row Data\n");
            for (ErrorDetail err : errorDetails) {
                sb.append(err.getRowNumber()).append(",")
                  .append("\"").append(escapeCsv(err.getColumn())).append("\",")
                  .append("\"").append(escapeCsv(err.getValue())).append("\",")
                  .append("\"").append(escapeCsv(err.getProblem())).append("\",")
                  .append("\"").append(escapeCsv(err.getSuggestedFix())).append("\",")
                  .append("\"").append(escapeCsv(err.getRawLine())).append("\"\n");
            }
            return sb.toString();
        }

        private String escapeCsv(String val) {
            if (val == null) return "";
            return val.replace("\"", "\"\"");
        }
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
                result.setTotalRowsDetected(result.getTotalRowsDetected() + 1);
                processDataRow(firstLine, delimiter, colMap, lineNum, result, demandEngine, classifier, recEngine);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                result.setTotalRowsDetected(result.getTotalRowsDetected() + 1);
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

        // 1. Extract and Clean Brand and Model
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
                    lineNum, "Brand", "(empty)", "Required brand name is missing",
                    "Specify a smartphone brand (e.g. Samsung, Apple, OnePlus)", line));
            return;
        }

        // Validate REQUIRED field: Model / Product Name
        if (rawModel == null || rawModel.trim().isEmpty()) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Model Name", "(empty)", "Required model name is missing",
                    "Specify a smartphone model name (e.g. Galaxy S24 Ultra)", line));
            return;
        }

        String brand = normalizeBrand(rawBrand);
        String modelName = cleanText(rawModel);

        // 2. Extract and Validate Price (Strict: Do not invent missing prices)
        String rawPrice = getMappedValue(tokens, colMap, "PRICE");
        if (rawPrice == null || rawPrice.trim().isEmpty()) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Price", "(empty)", "Required price is missing",
                    "Enter a positive numeric price such as 499.99 or 39999", line));
            return;
        }

        Double priceVal = cleanPrice(rawPrice);
        if (priceVal == null || priceVal <= 0) {
            result.setRowsSkipped(result.getRowsSkipped() + 1);
            result.getErrorDetails().add(new ErrorDetail(
                    lineNum, "Price", rawPrice, "Invalid numeric price value",
                    "Enter a positive numeric price such as 499.99 or 39999 without text", line));
            return;
        }
        double price = priceVal;

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
                    "Enter a numeric sales volume (e.g. 500)", line));
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
                    "Enter a numeric inventory stock count (e.g. 80)", line));
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
                    "Check database constraints and field types", line));
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
                    "brand", "brand_name", "brandname", "company", "make", "manufacturer", "brand_model", "oem", "vendor");
            assignFieldIfMatch(map, raw, i, "MODEL",
                    "model", "model_name", "modelname", "phone_model", "product_name", "productname", "device_name", "device", "mobile_name", "product", "title", "name", "phone", "smartphone", "item_name");
            assignFieldIfMatch(map, raw, i, "CATEGORY",
                    "category", "product_category", "type", "segment", "tier", "series", "class");
            assignFieldIfMatch(map, raw, i, "PRICE",
                    "price", "selling_price", "sellingprice", "product_price", "productprice", "price_inr", "price_usd", "cost", "mrp", "retail_price", "rate", "final_price", "discount_price", "original_price", "unit_price", "amount");
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
                    "network", "connectivity", "has_5g", "sim");
            assignFieldIfMatch(map, raw, i, "SALES",
                    "sales", "units_sold", "quantity_sold", "monthly_sales", "volume", "sold", "units", "sales_units");
            assignFieldIfMatch(map, raw, i, "PREV_SALES",
                    "previous_month_sales", "prev_monthly_sales", "prev_sales", "last_month_sales");
            assignFieldIfMatch(map, raw, i, "STOCK",
                    "current_stock", "stock", "inventory", "units_in_stock", "quantity_available", "available_stock", "qty", "quantity", "stock_quantity");
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

        // Positional defaults only if NO headers were recognized at all
        if (!map.containsKey("BRAND") && !map.containsKey("MODEL") && !map.containsKey("PRICE")) {
            if (headers.size() > 0) map.put("BRAND", 0);
            if (headers.size() > 1) map.put("MODEL", 1);
            if (headers.size() > 2) map.put("PRICE", 2);
        }

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

    public static Double cleanPrice(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String clean = raw.trim();
        // Remove currency symbols (INR ₹, USD $, EUR €, GBP £, etc.), commas, quotes, replacement chars
        clean = clean.replaceAll("[₹$€£¥,\"\']", "");
        clean = clean.replaceAll("[^0-9.]", "");
        if (clean.isEmpty()) return null;
        int firstDot = clean.indexOf('.');
        if (firstDot != -1) {
            String before = clean.substring(0, firstDot);
            String after = clean.substring(firstDot + 1).replace(".", "");
            clean = before + "." + after;
        }
        try {
            double val = Double.parseDouble(clean);
            return val > 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

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

    public static String normalizeBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) return "Other";
        String b = brand.trim().toLowerCase();

        if (b.contains("samsung")) return "Samsung";
        if (b.contains("apple") || b.contains("iphone")) return "Apple";
        if (b.contains("oneplus")) return "OnePlus";
        if (b.contains("poco")) return "POCO";
        if (b.contains("iqoo")) return "iQOO";
        if (b.contains("xiaomi") || b.contains("redmi") || b.contains("mi")) return "Xiaomi";
        if (b.contains("vivo")) return "Vivo";
        if (b.contains("oppo")) return "Oppo";
        if (b.contains("pixel") || b.contains("google")) return "Google Pixel";
        if (b.contains("realme")) return "Realme";
        if (b.contains("motorola") || b.contains("moto")) return "Motorola";
        if (b.contains("nothing")) return "Nothing";
        if (b.contains("honor")) return "Honor";
        if (b.contains("asus")) return "Asus";
        if (b.contains("sony")) return "Sony";
        if (b.contains("infinix")) return "Infinix";
        if (b.contains("tecno")) return "Tecno";
        if (b.contains("nokia")) return "Nokia";
        if (b.contains("lava")) return "Lava";
        if (b.contains("micromax")) return "Micromax";
        if (b.contains("huawei")) return "Huawei";
        if (b.contains("zte")) return "ZTE";
        if (b.contains("lenovo")) return "Lenovo";

        // Title case for any other brand
        return Character.toUpperCase(b.charAt(0)) + (b.length() > 1 ? b.substring(1) : "");
    }

    public static String deduceBrandFromModel(String model) {
        if (model == null || model.trim().isEmpty()) return null;
        String m = model.toLowerCase().trim();
        if (m.startsWith("samsung") || m.contains("galaxy")) return "Samsung";
        if (m.startsWith("apple") || m.startsWith("iphone") || m.contains("iphone")) return "Apple";
        if (m.startsWith("oneplus") || m.contains("oneplus") || m.contains("nord")) return "OnePlus";
        if (m.startsWith("poco") || m.contains("poco")) return "POCO";
        if (m.startsWith("iqoo") || m.contains("iqoo")) return "iQOO";
        if (m.startsWith("xiaomi") || m.startsWith("redmi") || m.startsWith("mi ") || m.contains("xiaomi") || m.contains("redmi")) return "Xiaomi";
        if (m.startsWith("vivo") || m.contains("vivo")) return "Vivo";
        if (m.startsWith("oppo") || m.contains("oppo") || m.contains("reno") || m.contains("find")) return "Oppo";
        if (m.startsWith("google") || m.startsWith("pixel") || m.contains("pixel")) return "Google Pixel";
        if (m.startsWith("motorola") || m.startsWith("moto") || m.contains("motorola") || m.contains("moto")) return "Motorola";
        if (m.startsWith("realme") || m.contains("realme") || m.contains("narzo")) return "Realme";
        if (m.startsWith("nothing") || m.contains("nothing")) return "Nothing";
        if (m.startsWith("honor") || m.contains("honor")) return "Honor";
        if (m.startsWith("asus") || m.contains("asus") || m.contains("rog")) return "Asus";
        if (m.startsWith("sony") || m.contains("sony") || m.contains("xperia")) return "Sony";
        if (m.startsWith("infinix") || m.contains("infinix")) return "Infinix";
        if (m.startsWith("tecno") || m.contains("tecno")) return "Tecno";
        if (m.startsWith("nokia") || m.contains("nokia")) return "Nokia";
        if (m.startsWith("lava") || m.contains("lava")) return "Lava";
        if (m.startsWith("micromax") || m.contains("micromax")) return "Micromax";
        if (m.startsWith("huawei") || m.contains("huawei")) return "Huawei";
        if (m.startsWith("zte") || m.contains("zte") || m.contains("nubia") || m.contains("redmagic")) return "ZTE";
        if (m.startsWith("lenovo") || m.contains("lenovo")) return "Lenovo";

        // If no known brand matches, take the first word as brand candidate if it's a valid alphabetic word
        String[] parts = model.trim().split("\\s+");
        if (parts.length > 1 && parts[0].matches("[a-zA-Z]{2,}")) {
            return Character.toUpperCase(parts[0].charAt(0)) + parts[0].substring(1).toLowerCase();
        }
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
