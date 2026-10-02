package com.mobilesales.analytics;

import com.mobilesales.model.MobileProduct;
import com.mobilesales.model.Sales;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SalesAnalytics implements Analytics interface for Sales forecasting and trend evaluation.
 * Demonstrates Polymorphism and Interface realization.
 */
public class SalesAnalytics implements Analytics<Sales, Map<String, Object>> {

    @Override
    public String getAnalyticsName() {
        return "Sales Velocity & Trend Analytics";
    }

    @Override
    public Map<String, Object> analyze(Sales sales) {
        Map<String, Object> metrics = new HashMap<>();
        if (sales == null) return metrics;

        // Sales Growth Calculation: ((Current Monthly - Previous Monthly) / Previous Monthly) * 100
        double growth = 0.0;
        if (sales.getPreviousMonthSales() > 0) {
            growth = ((double) (sales.getCurrentMonthlySales() - sales.getPreviousMonthSales()) / sales.getPreviousMonthSales()) * 100.0;
            growth = Math.round(growth * 10.0) / 10.0;
        }
        sales.setSalesGrowthPct(growth);

        // Trend Determination
        String trend;
        if (growth > 10.0) trend = "Upward";
        else if (growth < -5.0) trend = "Downward";
        else trend = "Stable";
        sales.setSalesTrend(trend);

        // Moving Average / Simple Exponential Smoothing for baseline future sales
        int predicted = (int) Math.round(sales.getCurrentMonthlySales() * (1.0 + (growth / 100.0)));
        if (predicted < 10) predicted = sales.getCurrentMonthlySales();
        sales.setPredictedFutureSales(predicted);

        metrics.put("totalUnitsSold", sales.getTotalUnitsSold());
        metrics.put("currentWeeklySales", sales.getCurrentWeeklySales());
        metrics.put("currentMonthlySales", sales.getCurrentMonthlySales());
        metrics.put("previousMonthSales", sales.getPreviousMonthSales());
        metrics.put("salesGrowthPct", growth);
        metrics.put("salesTrend", trend);
        metrics.put("predictedFutureSales", predicted);
        metrics.put("revenue", sales.getRevenue());
        metrics.put("weeklyHistory", sales.getWeeklyHistory());
        metrics.put("monthlyHistory", sales.getMonthlyHistory());

        return metrics;
    }

    @Override
    public Map<String, Object> getMetricsSummary(Sales sales) {
        return analyze(sales);
    }
}
