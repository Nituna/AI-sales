package com.mobilesales.analytics;

import com.mobilesales.model.Inventory;
import com.mobilesales.model.InventoryStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * InventoryAnalytics implements Analytics interface for inventory stock risk and status evaluation.
 */
public class InventoryAnalytics implements Analytics<Inventory, Map<String, Object>> {

    @Override
    public String getAnalyticsName() {
        return "Inventory Optimization & Buffer Analytics";
    }

    @Override
    public Map<String, Object> analyze(Inventory inventory) {
        Map<String, Object> metrics = new HashMap<>();
        if (inventory == null) return metrics;

        int current = inventory.getCurrentStock();
        int safety = inventory.getSafetyStock();
        int reorderLvl = inventory.getReorderLevel();

        InventoryStatus status;
        if (current <= safety) {
            status = InventoryStatus.REORDER_REQUIRED;
        } else if (current <= reorderLvl) {
            status = InventoryStatus.LOW_STOCK;
        } else if (current > (reorderLvl * 2.5)) {
            status = InventoryStatus.OVERSTOCK;
        } else {
            status = InventoryStatus.IN_STOCK;
        }
        inventory.setInventoryStatus(status);
        inventory.setAvailableStock(current);

        metrics.put("currentStock", current);
        metrics.put("safetyStock", safety);
        metrics.put("reorderLevel", reorderLvl);
        metrics.put("supplierLeadTimeDays", inventory.getSupplierLeadTimeDays());
        metrics.put("availableStock", inventory.getAvailableStock());
        metrics.put("inventoryStatus", status.getDisplayName());
        metrics.put("statusColor", status.getColorHex());
        metrics.put("isStockoutImminent", current <= safety);

        return metrics;
    }

    @Override
    public Map<String, Object> getMetricsSummary(Inventory inventory) {
        return analyze(inventory);
    }
}
