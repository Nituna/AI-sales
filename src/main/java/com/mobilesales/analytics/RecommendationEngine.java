package com.mobilesales.analytics;

import com.mobilesales.model.AISearchData;
import com.mobilesales.model.DemandPredictionResult;
import com.mobilesales.model.Inventory;
import com.mobilesales.model.ProductClassification;
import com.mobilesales.model.ReorderRecommendation;
import com.mobilesales.model.Sales;
import java.util.ArrayList;
import java.util.List;

/**
 * RecommendationEngine calculates smart inventory replenishment recommendations.
 * Reorder Point Formula: ReorderLevel = (Average Daily Sales * Lead Time) + Safety Stock
 * Recommended Reorder Quantity: (Predicted Demand + Safety Stock) - Current Stock
 */
public class RecommendationEngine {

    public ReorderRecommendation generateRecommendation(Sales sales, Inventory inventory,
                                                         AISearchData aiData, DemandPredictionResult demandResult,
                                                         ProductClassifier.ClassificationOutput classification) {
        int currentStock = inventory != null ? inventory.getCurrentStock() : 50;
        int safetyStock = inventory != null ? inventory.getSafetyStock() : 20;
        int leadTimeDays = inventory != null ? inventory.getSupplierLeadTimeDays() : 7;
        
        int currentMonthlySales = sales != null ? sales.getCurrentMonthlySales() : 45;
        double avgDailySales = Math.round((currentMonthlySales / 30.0) * 10.0) / 10.0;
        if (avgDailySales <= 0) avgDailySales = 1.5;

        int predictedDemand = demandResult != null ? demandResult.getPredictedDemand() : (int) Math.round(currentMonthlySales * 1.2);

        // Standard Reorder Point: (Daily Demand * Lead Time) + Safety Stock
        int calculatedReorderLevel = (int) Math.ceil((avgDailySales * leadTimeDays) + safetyStock);
        if (inventory != null && inventory.getReorderLevel() > 0) {
            calculatedReorderLevel = inventory.getReorderLevel();
        }

        // Determine if reorder is recommended
        // Triggers:
        // 1. Current stock <= reorder level
        // 2. High predicted demand creates a stockout deficit during the lead time cycle
        // 3. Potential Fast Moving product flag
        boolean stockBelowReorder = currentStock <= calculatedReorderLevel;
        boolean demandSurgeDeficit = (predictedDemand + safetyStock) > (currentStock * 1.4);
        boolean isPotentialFast = classification != null && classification.getClassification() == ProductClassification.POTENTIAL_FAST_MOVING;

        boolean reorderNeeded = stockBelowReorder || demandSurgeDeficit || (isPotentialFast && currentStock < (safetyStock * 2));

        int recommendedQty = 0;
        String status;
        List<String> reasons = new ArrayList<>();

        if (reorderNeeded) {
            status = "REORDER RECOMMENDED";
            // Recommended Reorder = (Predicted Demand + Safety Stock) - Current Stock
            recommendedQty = Math.max(15, (predictedDemand + safetyStock) - currentStock);

            // Build explanatory checklist ("Why?")
            if (sales != null && sales.getSalesGrowthPct() > 0) {
                reasons.add(String.format("Sales increased by %.0f%%", sales.getSalesGrowthPct()));
            } else if (sales != null && sales.getSalesGrowthPct() == 0) {
                reasons.add("Sales velocity remains sustained at target threshold");
            }

            if (aiData != null && aiData.getVisibilityScore() >= 65.0) {
                reasons.add("Visibility increased (" + Math.round(aiData.getVisibilityScore()) + "% search query appearance)");
            }

            if (aiData != null && aiData.getEngagementScore() >= 65.0) {
                reasons.add("Engagement increased (" + Math.round(aiData.getEngagementScore()) + "% customer query interactions)");
            }

            if (stockBelowReorder) {
                reasons.add(String.format("Current stock (%d units) is below reorder level (%d units)", currentStock, calculatedReorderLevel));
            }

            if (demandResult != null && demandResult.getPredictedDemand() > demandResult.getCurrentDemand()) {
                reasons.add(String.format("Predicted demand is increasing (%d -> %d units, +%.1f%%)",
                        demandResult.getCurrentDemand(), demandResult.getPredictedDemand(), demandResult.getDemandGrowthPct()));
            }

            if (isPotentialFast) {
                reasons.add("Early warning: Product flagged as 'Potential Fast-Moving' due to search surge");
            }
        } else {
            status = "STOCK ADEQUATE";
            recommendedQty = 0;
            reasons.add(String.format("Current stock (%d units) comfortably exceeds reorder level (%d units)", currentStock, calculatedReorderLevel));
            reasons.add("Lead time coverage (" + leadTimeDays + " days) is fully protected by existing inventory");
            reasons.add("No immediate supplier replenishment order required");
        }

        boolean urgent = currentStock <= safetyStock;

        return new ReorderRecommendation(
                0,
                sales != null ? sales.getProductId() : 0,
                status,
                currentStock,
                predictedDemand,
                calculatedReorderLevel,
                recommendedQty,
                avgDailySales,
                leadTimeDays,
                safetyStock,
                reasons,
                urgent
        );
    }
}
