package com.mobilesales.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing Smart Inventory Reorder Recommendations.
 */
public class ReorderRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int recommendationId;
    private int productId;
    private String status; // "REORDER RECOMMENDED", "STOCK ADEQUATE", "OVERSTOCK MONITOR"
    private int currentStock;
    private int predictedDemand;
    private int reorderLevel;
    private int recommendedReorderQty;
    private double avgDailySales;
    private int supplierLeadTimeDays;
    private int safetyStock;
    private List<String> reasonsList;
    private boolean urgent;

    public ReorderRecommendation() {
        this.reasonsList = new ArrayList<>();
    }

    public ReorderRecommendation(int recommendationId, int productId, String status,
                                 int currentStock, int predictedDemand, int reorderLevel,
                                 int recommendedReorderQty, double avgDailySales,
                                 int supplierLeadTimeDays, int safetyStock,
                                 List<String> reasonsList, boolean urgent) {
        this.recommendationId = recommendationId;
        this.productId = productId;
        this.status = status;
        this.currentStock = currentStock;
        this.predictedDemand = predictedDemand;
        this.reorderLevel = reorderLevel;
        this.recommendedReorderQty = recommendedReorderQty;
        this.avgDailySales = avgDailySales;
        this.supplierLeadTimeDays = supplierLeadTimeDays;
        this.safetyStock = safetyStock;
        this.reasonsList = reasonsList != null ? reasonsList : new ArrayList<>();
        this.urgent = urgent;
    }

    public int getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(int recommendationId) {
        this.recommendationId = recommendationId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(int predictedDemand) {
        this.predictedDemand = predictedDemand;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public int getRecommendedReorderQty() {
        return recommendedReorderQty;
    }

    public void setRecommendedReorderQty(int recommendedReorderQty) {
        this.recommendedReorderQty = recommendedReorderQty;
    }

    public double getAvgDailySales() {
        return avgDailySales;
    }

    public void setAvgDailySales(double avgDailySales) {
        this.avgDailySales = avgDailySales;
    }

    public int getSupplierLeadTimeDays() {
        return supplierLeadTimeDays;
    }

    public void setSupplierLeadTimeDays(int supplierLeadTimeDays) {
        this.supplierLeadTimeDays = supplierLeadTimeDays;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(int safetyStock) {
        this.safetyStock = safetyStock;
    }

    public List<String> getReasonsList() {
        return reasonsList;
    }

    public void setReasonsList(List<String> reasonsList) {
        this.reasonsList = reasonsList;
    }

    public void addReason(String reason) {
        this.reasonsList.add(reason);
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }
}
