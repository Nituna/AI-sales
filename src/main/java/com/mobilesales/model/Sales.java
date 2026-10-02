package com.mobilesales.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing Sales records and time-series performance for a product.
 */
public class Sales implements Serializable {
    private static final long serialVersionUID = 1L;

    private int salesId;
    private int productId;
    private LocalDate recordDate;
    private int totalUnitsSold;
    private int currentWeeklySales;
    private int currentMonthlySales;
    private int previousMonthSales;
    private double revenue;
    private double salesGrowthPct;
    private String salesTrend; // "Upward", "Stable", "Downward"
    private int predictedFutureSales;
    private List<Integer> weeklyHistory; // Recent 8-12 weeks
    private List<Integer> monthlyHistory; // Recent 6-12 months

    public Sales() {
        this.weeklyHistory = new ArrayList<>();
        this.monthlyHistory = new ArrayList<>();
    }

    public Sales(int salesId, int productId, LocalDate recordDate, int totalUnitsSold,
                 int currentWeeklySales, int currentMonthlySales, int previousMonthSales,
                 double revenue, double salesGrowthPct, String salesTrend, int predictedFutureSales) {
        this.salesId = salesId;
        this.productId = productId;
        this.recordDate = recordDate;
        this.totalUnitsSold = totalUnitsSold;
        this.currentWeeklySales = currentWeeklySales;
        this.currentMonthlySales = currentMonthlySales;
        this.previousMonthSales = previousMonthSales;
        this.revenue = revenue;
        this.salesGrowthPct = salesGrowthPct;
        this.salesTrend = salesTrend;
        this.predictedFutureSales = predictedFutureSales;
        this.weeklyHistory = new ArrayList<>();
        this.monthlyHistory = new ArrayList<>();
    }

    public int getSalesId() {
        return salesId;
    }

    public void setSalesId(int salesId) {
        this.salesId = salesId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public int getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(int totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public int getCurrentWeeklySales() {
        return currentWeeklySales;
    }

    public void setCurrentWeeklySales(int currentWeeklySales) {
        this.currentWeeklySales = currentWeeklySales;
    }

    public int getCurrentMonthlySales() {
        return currentMonthlySales;
    }

    public void setCurrentMonthlySales(int currentMonthlySales) {
        this.currentMonthlySales = currentMonthlySales;
    }

    public int getPreviousMonthSales() {
        return previousMonthSales;
    }

    public void setPreviousMonthSales(int previousMonthSales) {
        this.previousMonthSales = previousMonthSales;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public double getSalesGrowthPct() {
        return salesGrowthPct;
    }

    public void setSalesGrowthPct(double salesGrowthPct) {
        this.salesGrowthPct = salesGrowthPct;
    }

    public String getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(String salesTrend) {
        this.salesTrend = salesTrend;
    }

    public int getPredictedFutureSales() {
        return predictedFutureSales;
    }

    public void setPredictedFutureSales(int predictedFutureSales) {
        this.predictedFutureSales = predictedFutureSales;
    }

    public List<Integer> getWeeklyHistory() {
        return weeklyHistory;
    }

    public void setWeeklyHistory(List<Integer> weeklyHistory) {
        this.weeklyHistory = weeklyHistory;
    }

    public List<Integer> getMonthlyHistory() {
        return monthlyHistory;
    }

    public void setMonthlyHistory(List<Integer> monthlyHistory) {
        this.monthlyHistory = monthlyHistory;
    }
}
