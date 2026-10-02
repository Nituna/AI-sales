package com.mobilesales.io;

import com.mobilesales.dao.*;
import com.mobilesales.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * CSVDataExporter implements Java File I/O for exporting processed inventory and analytical models.
 */
public class CSVDataExporter {

    private final MobileProductDAO productDAO;
    private final SalesDAO salesDAO;
    private final InventoryDAO inventoryDAO;
    private final AISearchDAO aiDAO;
    private final AnalyticsDAO analyticsDAO;
    private final RecommendationDAO recommendationDAO;

    public CSVDataExporter() {
        this.productDAO = new MobileProductDAO();
        this.salesDAO = new SalesDAO();
        this.inventoryDAO = new InventoryDAO();
        this.aiDAO = new AISearchDAO();
        this.analyticsDAO = new AnalyticsDAO();
        this.recommendationDAO = new RecommendationDAO();
    }

    public String generateCSVString() throws SQLException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        writeCSV(pw);
        return sw.toString();
    }

    public void exportToFile(File destination) throws IOException, SQLException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destination))) {
            writeCSV(pw);
        }
    }

    private void writeCSV(PrintWriter pw) throws SQLException {
        // CSV Header
        pw.println("ProductId,Brand,ModelName,Category,Price,RAM,Storage,Processor,Battery,CurrentStock,SafetyStock,ReorderLevel,LeadTimeDays,InventoryStatus,MonthlySales,GrowthPct,SalesTrend,VisibilityScore,AccuracyScore,EngagementScore,PredictedDemand,DemandTrend,Classification,ReorderStatus,RecommendedOrderQty");

        List<MobileProduct> products = productDAO.getAllProducts();
        for (MobileProduct p : products) {
            int pid = p.getProductId();
            Sales s = salesDAO.getSalesByProductId(pid);
            Inventory inv = inventoryDAO.getInventoryByProductId(pid);
            AISearchData ai = aiDAO.getAISearchDataByProductId(pid);
            Map<String, Object> analytics = analyticsDAO.getAnalyticsByProductId(pid);
            ReorderRecommendation rec = recommendationDAO.getRecommendationByProductId(pid);

            pw.printf("%d,\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,\"%s\",%d,%.1f,\"%s\",%.1f,%.1f,%.1f,%d,\"%s\",\"%s\",\"%s\",%d%n",
                    pid,
                    escape(p.getBrand()),
                    escape(p.getModelName()),
                    escape(p.getCategory()),
                    p.getPrice(),
                    escape(p.getRam()),
                    escape(p.getStorage()),
                    escape(p.getProcessor()),
                    escape(p.getBattery()),
                    inv != null ? inv.getCurrentStock() : 0,
                    inv != null ? inv.getSafetyStock() : 0,
                    inv != null ? inv.getReorderLevel() : 0,
                    inv != null ? inv.getSupplierLeadTimeDays() : 0,
                    inv != null && inv.getInventoryStatus() != null ? inv.getInventoryStatus().getDisplayName() : "N/A",
                    s != null ? s.getCurrentMonthlySales() : 0,
                    s != null ? s.getSalesGrowthPct() : 0.0,
                    s != null && s.getSalesTrend() != null ? s.getSalesTrend() : "Stable",
                    ai != null ? ai.getVisibilityScore() : 0.0,
                    ai != null ? ai.getAccuracyScore() : 0.0,
                    ai != null ? ai.getEngagementScore() : 0.0,
                    analytics != null && analytics.get("predictedDemand") != null ? (int) analytics.get("predictedDemand") : 0,
                    analytics != null && analytics.get("demandTrend") != null ? analytics.get("demandTrend").toString() : "Stable",
                    analytics != null && analytics.get("classification") != null ? analytics.get("classification").toString() : "Medium Moving",
                    rec != null && rec.getStatus() != null ? rec.getStatus() : "STOCK ADEQUATE",
                    rec != null ? rec.getRecommendedReorderQty() : 0
            );
        }
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
