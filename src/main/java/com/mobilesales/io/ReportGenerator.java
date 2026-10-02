package com.mobilesales.io;

import com.mobilesales.dao.*;
import com.mobilesales.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ReportGenerator generates comprehensive business intelligence and supply chain audit reports.
 * Implements Java File I/O for report generation and exports in Markdown and HTML.
 */
public class ReportGenerator {

    private final MobileProductDAO productDAO;
    private final SalesDAO salesDAO;
    private final InventoryDAO inventoryDAO;
    private final AISearchDAO aiDAO;
    private final AnalyticsDAO analyticsDAO;
    private final RecommendationDAO recommendationDAO;

    public ReportGenerator() {
        this.productDAO = new MobileProductDAO();
        this.salesDAO = new SalesDAO();
        this.inventoryDAO = new InventoryDAO();
        this.aiDAO = new AISearchDAO();
        this.analyticsDAO = new AnalyticsDAO();
        this.recommendationDAO = new RecommendationDAO();
    }

    public String generateMarkdownReport() throws SQLException {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String now = LocalDateTime.now().format(dtf);

        sb.append("# AI-POWERED MOBILE INVENTORY, SALES & REORDER ANALYTICS AUDIT\n\n");
        sb.append("**Generated On:** ").append(now).append("\n");
        sb.append("**System:** Intelligent Supply Chain Intelligence Engine (Java OOP + Data Science)\n\n");
        sb.append("---\n\n");

        List<MobileProduct> products = productDAO.getAllProducts();
        int totalProducts = products.size();
        int totalSold = 0;
        int totalStock = 0;
        int reordersRequired = 0;
        double totalVis = 0.0;
        double totalAcc = 0.0;
        double totalEng = 0.0;

        for (MobileProduct p : products) {
            int pid = p.getProductId();
            Sales s = salesDAO.getSalesByProductId(pid);
            Inventory inv = inventoryDAO.getInventoryByProductId(pid);
            AISearchData ai = aiDAO.getAISearchDataByProductId(pid);
            ReorderRecommendation rec = recommendationDAO.getRecommendationByProductId(pid);

            if (s != null) totalSold += s.getTotalUnitsSold();
            if (inv != null) totalStock += inv.getCurrentStock();
            if (ai != null) {
                totalVis += ai.getVisibilityScore();
                totalAcc += ai.getAccuracyScore();
                totalEng += ai.getEngagementScore();
            }
            if (rec != null && "REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus())) {
                reordersRequired++;
            }
        }

        double avgVis = totalProducts > 0 ? totalVis / totalProducts : 0.0;
        double avgAcc = totalProducts > 0 ? totalAcc / totalProducts : 0.0;
        double avgEng = totalProducts > 0 ? totalEng / totalProducts : 0.0;

        sb.append("## 1. Executive Summary\n\n");
        sb.append("| Metric | Value | Benchmark / Health Status |\n");
        sb.append("|---|---|---|\n");
        sb.append(String.format("| **Total Tracked Mobile Models** | %d devices | Active Catalog |\n", totalProducts));
        sb.append(String.format("| **Total Historical Units Sold** | %,d units | High Market Circulation |\n", totalSold));
        sb.append(String.format("| **Current Warehouse Stock** | %,d units | Live Buffer |\n", totalStock));
        sb.append(String.format("| **Products Requiring Reorder** | %d items | 🔴 Immediate Action Required |\n", reordersRequired));
        sb.append(String.format("| **Average AI Visibility Score** | %.1f%% | %s |\n", avgVis, avgVis >= 70 ? "Optimal Reach" : "Underperforming"));
        sb.append(String.format("| **Average Information Accuracy** | %.1f%% | %s |\n", avgAcc, avgAcc >= 90 ? "Verified Specs" : "Spec Discrepancies"));
        sb.append(String.format("| **Average Customer Engagement** | %.1f%% | High Intent Signal |\n", avgEng));
        sb.append("\n");

        sb.append("## 2. Priority Reorder Recommendations\n\n");
        sb.append("Items requiring urgent replenishment based on lead time, safety stock, and AI-predicted demand:\n\n");

        int recCount = 0;
        for (MobileProduct p : products) {
            int pid = p.getProductId();
            ReorderRecommendation rec = recommendationDAO.getRecommendationByProductId(pid);
            if (rec != null && "REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus())) {
                recCount++;
                Inventory inv = inventoryDAO.getInventoryByProductId(pid);
                Map<String, Object> an = analyticsDAO.getAnalyticsByProductId(pid);

                sb.append(String.format("### %d. %s %s (%s)\n", recCount, p.getBrand(), p.getModelName(),
                        an != null ? an.get("classification") : "Standard"));
                sb.append("- **Current Stock:** ").append(rec.getCurrentStock()).append(" units\n");
                sb.append("- **Reorder Point:** ").append(rec.getReorderLevel()).append(" units\n");
                sb.append("- **Predicted Demand:** ").append(rec.getPredictedDemand()).append(" units\n");
                sb.append("- **Recommended Replenishment:** **").append(rec.getRecommendedReorderQty()).append(" units**\n");
                sb.append("- **Why? Rationale Checklist:**\n");
                for (String reason : rec.getReasonsList()) {
                    sb.append("  - [x] ").append(reason).append("\n");
                }
                sb.append("\n");
            }
        }

        if (recCount == 0) {
            sb.append("*All tracked smartphone stock levels are currently within safe thresholds.*\n\n");
        }

        sb.append("## 3. Product Catalog Breakdown\n\n");
        sb.append("| Brand | Model | Price | Stock | Monthly Sales | Growth | Visibility | Accuracy | Engagement | Classification | Action |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|---|\n");

        for (MobileProduct p : products) {
            int pid = p.getProductId();
            Sales s = salesDAO.getSalesByProductId(pid);
            Inventory inv = inventoryDAO.getInventoryByProductId(pid);
            AISearchData ai = aiDAO.getAISearchDataByProductId(pid);
            Map<String, Object> an = analyticsDAO.getAnalyticsByProductId(pid);
            ReorderRecommendation rec = recommendationDAO.getRecommendationByProductId(pid);

            sb.append(String.format("| %s | %s | $%.2f | %d | %d | %+.1f%% | %.1f%% | %.1f%% | %.1f%% | %s | **%s** |\n",
                    p.getBrand(),
                    p.getModelName(),
                    p.getPrice(),
                    inv != null ? inv.getCurrentStock() : 0,
                    s != null ? s.getCurrentMonthlySales() : 0,
                    s != null ? s.getSalesGrowthPct() : 0.0,
                    ai != null ? ai.getVisibilityScore() : 0.0,
                    ai != null ? ai.getAccuracyScore() : 0.0,
                    ai != null ? ai.getEngagementScore() : 0.0,
                    an != null ? an.get("classification") : "N/A",
                    rec != null && "REORDER RECOMMENDED".equalsIgnoreCase(rec.getStatus()) ? "REORDER" : "OK"
            ));
        }

        sb.append("\n---\n");
        sb.append("*End of Report - AI Mobile Sales & Inventory Intelligence System*\n");

        return sb.toString();
    }

    public void saveReportToFile(File targetFile) throws IOException, SQLException {
        String content = generateMarkdownReport();
        try (FileWriter fw = new FileWriter(targetFile)) {
            fw.write(content);
        }
    }
}
