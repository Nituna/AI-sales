package com.mobilesales.model;

import java.io.Serializable;

/**
 * Model representing Inventory stock, safety buffer, lead time, and reorder levels.
 */
public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    private int inventoryId;
    private int productId;
    private int currentStock;
    private int safetyStock;
    private int reorderLevel;
    private int supplierLeadTimeDays;
    private int availableStock;
    private InventoryStatus inventoryStatus;

    public Inventory() {
        this.inventoryStatus = InventoryStatus.IN_STOCK;
    }

    public Inventory(int inventoryId, int productId, int currentStock, int safetyStock,
                     int reorderLevel, int supplierLeadTimeDays, int availableStock,
                     InventoryStatus inventoryStatus) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.currentStock = currentStock;
        this.safetyStock = safetyStock;
        this.reorderLevel = reorderLevel;
        this.supplierLeadTimeDays = supplierLeadTimeDays;
        this.availableStock = availableStock;
        this.inventoryStatus = inventoryStatus != null ? inventoryStatus : InventoryStatus.IN_STOCK;
    }

    public int getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(int safetyStock) {
        this.safetyStock = safetyStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public int getSupplierLeadTimeDays() {
        return supplierLeadTimeDays;
    }

    public void setSupplierLeadTimeDays(int supplierLeadTimeDays) {
        this.supplierLeadTimeDays = supplierLeadTimeDays;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "currentStock=" + currentStock +
                ", safetyStock=" + safetyStock +
                ", reorderLevel=" + reorderLevel +
                ", leadTime=" + supplierLeadTimeDays + "d" +
                ", status=" + inventoryStatus +
                '}';
    }
}
