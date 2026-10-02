package com.mobilesales.analytics;

import com.mobilesales.model.Inventory;

/**
 * InventoryAnalyzer computes safety stock, reorder levels, and turnover metrics.
 */
public class InventoryAnalyzer {

    /**
     * Calculates the Reorder Point (ROP) = (Average Daily Demand * Lead Time Days) + Safety Stock
     */
    public int calculateReorderLevel(double avgDailyDemand, int leadTimeDays, int safetyStock) {
        int leadTimeDemand = (int) Math.ceil(avgDailyDemand * leadTimeDays);
        return leadTimeDemand + safetyStock;
    }

    /**
     * Calculates Safety Stock using max lead time / demand buffer variance
     */
    public int calculateDynamicSafetyStock(double avgDailyDemand, int maxLeadTimeDays, int avgLeadTimeDays) {
        int leadTimeVariance = Math.max(2, maxLeadTimeDays - avgLeadTimeDays);
        return (int) Math.ceil(avgDailyDemand * leadTimeVariance * 1.65); // 95% service level factor
    }

    /**
     * Evaluates inventory health and returns a risk index (0 - 100)
     */
    public double calculateStockoutRisk(Inventory inventory, double avgDailyDemand) {
        if (avgDailyDemand <= 0) return 0.0;
        double daysOfSupply = inventory.getCurrentStock() / avgDailyDemand;
        if (daysOfSupply <= inventory.getSupplierLeadTimeDays()) {
            return 95.0; // High imminent stockout risk
        } else if (daysOfSupply <= (inventory.getSupplierLeadTimeDays() * 1.5)) {
            return 60.0; // Moderate risk
        }
        return 10.0; // Low risk
    }
}
