package com.mobilesales.model;

import java.io.Serializable;
import java.util.List;

/**
 * Model representing Demand Analysis and Forecasting output.
 */
public class DemandPredictionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int productId;
    private int currentDemand;
    private int predictedDemand;
    private double demandGrowthPct;
    private String demandTrend; // "Surging", "Gradual Increase", "Stable", "Declining"
    private double baselineSalesWeight;
    private double aiSignalModifier;
    private double signalCorrelationCoeff;
    private boolean aiSignalUseful;
    private String analyticalInsights;
    private List<Integer> projectedDemandCurve;

    public DemandPredictionResult() {}

    public DemandPredictionResult(int productId, int currentDemand, int predictedDemand,
                                  double demandGrowthPct, String demandTrend,
                                  double baselineSalesWeight, double aiSignalModifier,
                                  double signalCorrelationCoeff, boolean aiSignalUseful,
                                  String analyticalInsights) {
        this.productId = productId;
        this.currentDemand = currentDemand;
        this.predictedDemand = predictedDemand;
        this.demandGrowthPct = demandGrowthPct;
        this.demandTrend = demandTrend;
        this.baselineSalesWeight = baselineSalesWeight;
        this.aiSignalModifier = aiSignalModifier;
        this.signalCorrelationCoeff = signalCorrelationCoeff;
        this.aiSignalUseful = aiSignalUseful;
        this.analyticalInsights = analyticalInsights;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCurrentDemand() {
        return currentDemand;
    }

    public void setCurrentDemand(int currentDemand) {
        this.currentDemand = currentDemand;
    }

    public int getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(int predictedDemand) {
        this.predictedDemand = predictedDemand;
    }

    public double getDemandGrowthPct() {
        return demandGrowthPct;
    }

    public void setDemandGrowthPct(double demandGrowthPct) {
        this.demandGrowthPct = demandGrowthPct;
    }

    public String getDemandTrend() {
        return demandTrend;
    }

    public void setDemandTrend(String demandTrend) {
        this.demandTrend = demandTrend;
    }

    public double getBaselineSalesWeight() {
        return baselineSalesWeight;
    }

    public void setBaselineSalesWeight(double baselineSalesWeight) {
        this.baselineSalesWeight = baselineSalesWeight;
    }

    public double getAiSignalModifier() {
        return aiSignalModifier;
    }

    public void setAiSignalModifier(double aiSignalModifier) {
        this.aiSignalModifier = aiSignalModifier;
    }

    public double getSignalCorrelationCoeff() {
        return signalCorrelationCoeff;
    }

    public void setSignalCorrelationCoeff(double signalCorrelationCoeff) {
        this.signalCorrelationCoeff = signalCorrelationCoeff;
    }

    public boolean isAiSignalUseful() {
        return aiSignalUseful;
    }

    public void setAiSignalUseful(boolean aiSignalUseful) {
        this.aiSignalUseful = aiSignalUseful;
    }

    public String getAnalyticalInsights() {
        return analyticalInsights;
    }

    public void setAnalyticalInsights(String analyticalInsights) {
        this.analyticalInsights = analyticalInsights;
    }

    public List<Integer> getProjectedDemandCurve() {
        return projectedDemandCurve;
    }

    public void setProjectedDemandCurve(List<Integer> projectedDemandCurve) {
        this.projectedDemandCurve = projectedDemandCurve;
    }
}
