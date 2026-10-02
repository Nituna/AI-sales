package com.mobilesales.analytics;

import com.mobilesales.model.AISearchData;
import com.mobilesales.model.ProductClassification;
import com.mobilesales.model.Sales;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductClassifier categorizes mobile devices into market velocity classes:
 * 1. Fast Moving
 * 2. Medium Moving
 * 3. Slow Moving
 * 4. Potential Fast Moving (AI early warning signal)
 */
public class ProductClassifier {

    public static class ClassificationOutput {
        private final ProductClassification classification;
        private final String systemMessage;
        private final String rationale;
        private final boolean isAlertTriggered;

        public ClassificationOutput(ProductClassification classification, String systemMessage, String rationale, boolean isAlertTriggered) {
            this.classification = classification;
            this.systemMessage = systemMessage;
            this.rationale = rationale;
            this.isAlertTriggered = isAlertTriggered;
        }

        public ProductClassification getClassification() {
            return classification;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public String getRationale() {
            return rationale;
        }

        public boolean isAlertTriggered() {
            return isAlertTriggered;
        }
    }

    public ClassificationOutput classifyProduct(Sales sales, AISearchData aiData) {
        int currentSales = sales != null ? sales.getCurrentMonthlySales() : 0;
        double growth = sales != null ? sales.getSalesGrowthPct() : 0.0;
        double visibility = aiData != null ? aiData.getVisibilityScore() : 50.0;
        double engagement = aiData != null ? aiData.getEngagementScore() : 50.0;

        // Requirement 7: Potential Fast Moving
        // If current sales are relatively low/moderate (< 80 units), but visibility (> 70%) and engagement (> 70%) are high/increasing:
        if (currentSales < 85 && visibility >= 70.0 && engagement >= 68.0) {
            return new ClassificationOutput(
                    ProductClassification.POTENTIAL_FAST_MOVING,
                    "Potential future demand increase detected.",
                    "Current sales volume is relatively low (" + currentSales + " units), but search visibility (" +
                            Math.round(visibility) + "%) and customer engagement (" + Math.round(engagement) + "%) are surging.",
                    true
            );
        }

        // Fast Moving: High sales volume (> 110 units) or growth > 15% with high sales
        if (currentSales >= 110 || (currentSales >= 80 && growth > 15.0)) {
            return new ClassificationOutput(
                    ProductClassification.FAST_MOVING,
                    "High velocity product with consistent demand.",
                    "Strong unit volume (" + currentSales + " units/mo) and positive momentum.",
                    false
            );
        }

        // Medium Moving: Balanced performance (50 - 109 units)
        if (currentSales >= 50) {
            return new ClassificationOutput(
                    ProductClassification.MEDIUM_MOVING,
                    "Steady turnover with stable inventory replenishment cycles.",
                    "Balanced monthly sales (" + currentSales + " units) within standard tolerance.",
                    false
            );
        }

        // Slow Moving: Low volume (< 50 units) without high search lift
        return new ClassificationOutput(
                ProductClassification.SLOW_MOVING,
                "Low sales turnover detected; risk of overstock.",
                "Subdued monthly velocity (" + currentSales + " units) and moderate/low search traction.",
                true
        );
    }
}
