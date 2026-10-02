package com.mobilesales.model;

import java.time.LocalDate;

/**
 * MobileProduct represents a smartphone in inventory.
 * Demonstrates Inheritance (extends Product) and Polymorphism.
 */
public class MobileProduct extends Product {
    private static final long serialVersionUID = 1L;

    private String ram;
    private String storage;
    private String processor;
    private String battery;
    private String display;
    private String camera;
    private String os;
    private String network;

    public MobileProduct() {
        super();
        this.category = "Smartphone";
    }

    public MobileProduct(int productId, String brand, String modelName, String category, double price,
                         LocalDate launchDate, String ram, String storage, String processor,
                         String battery, String display, String camera, String os, String network) {
        super(productId, brand, modelName, category != null ? category : "Smartphone", price, launchDate);
        this.ram = ram;
        this.storage = storage;
        this.processor = processor;
        this.battery = battery;
        this.display = display;
        this.camera = camera;
        this.os = os;
        this.network = network;
    }

    // Overriding abstract methods - demonstrating Polymorphism
    @Override
    public String getProductType() {
        return "Mobile Device / Smartphone";
    }

    @Override
    public String getFormattedSpecifications() {
        return String.format("%s | %s | %s | %s | %s | %s | %s",
                display, processor, ram, storage, camera, battery, os);
    }

    @Override
    public double calculateDepreciatedPrice(int monthsInMarket) {
        // Industry mobile depreciation curve: approx 1.5% per month up to 50% max
        double factor = Math.max(0.50, 1.0 - (monthsInMarket * 0.015));
        return Math.round(this.price * factor * 100.0) / 100.0;
    }

    // Getters and Setters
    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    @Override
    public String toString() {
        return "MobileProduct{" +
                "brand='" + brand + '\'' +
                ", modelName='" + modelName + '\'' +
                ", price=" + price +
                ", processor='" + processor + '\'' +
                ", ram='" + ram + '\'' +
                ", storage='" + storage + '\'' +
                ", battery='" + battery + '\'' +
                '}';
    }
}
