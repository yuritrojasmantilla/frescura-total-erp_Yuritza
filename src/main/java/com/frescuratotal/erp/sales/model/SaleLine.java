package com.frescuratotal.erp.sales.model;

public final class SaleLine {
    private final int lotId, productId;
    private final String lotNumber, productName, unit;
    private final double quantity,
            unitPrice,
            discountRate,
            discountAmount,
            taxRate,
            taxAmount,
            subtotal,
            lineTotal;

    public SaleLine(
            int lotId,
            int productId,
            String lotNumber,
            String productName,
            String unit,
            double quantity,
            double unitPrice,
            double discountRate,
            double discountAmount,
            double taxRate,
            double taxAmount,
            double subtotal,
            double lineTotal) {
        this.lotId = lotId;
        this.productId = productId;
        this.lotNumber = lotNumber;
        this.productName = productName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.subtotal = subtotal;
        this.lineTotal = lineTotal;
    }

    public int getLotId() {
        return lotId;
    }

    public int getProductId() {
        return productId;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public String getProductName() {
        return productName;
    }

    public String getUnit() {
        return unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getLineTotal() {
        return lineTotal;
    }
}
