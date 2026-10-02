package com.mobilesales.analytics;

import java.util.Map;

/**
 * Core Analytics Interface.
 * Demonstrates Abstraction, Polymorphism, and Generic Design in Java OOP.
 * Implemented by SalesAnalytics, InventoryAnalytics, AIAnalytics, and DemandAnalytics.
 */
public interface Analytics<T, R> {
    
    /**
     * Executes the analytical model and returns the computed result.
     * @param input Data payload or model instance to analyze
     * @return Computed analytical domain result
     */
    R analyze(T input);

    /**
     * Returns the human-readable identifier for this analytical module.
     * @return Module name
     */
    String getAnalyticsName();

    /**
     * Generates a key-value metrics summary for reporting and dashboards.
     * @param input Data payload
     * @return Map of metric keys to values
     */
    Map<String, Object> getMetricsSummary(T input);
}
