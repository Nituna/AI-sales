package com.mobilesales.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Abstract Base Class Product.
 * Demonstrates Abstraction & Encapsulation.
 * Base class for all inventory product categories.
 */
public abstract class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int productId;
    protected String brand;
    protected String modelName;
    protected String category;
    protected double price;
    protected LocalDate launchDate;

    public Product() {
    }

    public Product(int productId, String brand, String modelName, String category, double price, LocalDate launchDate) {
        this.productId = productId;
        this.brand = brand;
        this.modelName = modelName;
        this.category = category;
        this.price = price;
        this.launchDate = launchDate;
    }

    // Abstract methods demonstrating Abstraction
    public abstract String getProductType();
    public abstract String getFormattedSpecifications();
    public abstract double calculateDepreciatedPrice(int monthsInMarket);

    // Concrete getters and setters demonstrating Encapsulation
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDate getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(LocalDate launchDate) {
        this.launchDate = launchDate;
    }

    @Override
    public String toString() {
        return brand + " " + modelName + " ($" + price + ")";
    }
}
