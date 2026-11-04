package com.frescuratotal.erp.inventory.model;

import com.frescuratotal.erp.common.Validation;

public class Product {
    private final int id;
    private String name;
    private String description;
    private int categoryId;
    private final String unit;
    private double basePrice;

    public Product(
            int id,
            String name,
            String description,
            int categoryId,
            String unit,
            double basePrice) {
        if (id < 0) throw new IllegalArgumentException("Product ID cannot be negative");
        this.id = id;
        setName(name);
        setDescription(description);
        setCategoryId(categoryId);
        this.unit = Validation.text(unit, "Unit", 20);
        setBasePrice(basePrice);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getUnit() {
        return unit;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setName(String name) {
        this.name = Validation.text(name, "Name", 255);
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = Validation.positiveId(categoryId, "Category ID");
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = Validation.decimal(basePrice, "Base price", false);
    }
}
