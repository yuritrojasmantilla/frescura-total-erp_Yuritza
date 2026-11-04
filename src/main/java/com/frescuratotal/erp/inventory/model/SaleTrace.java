package com.frescuratotal.erp.inventory.model;

import java.time.LocalDateTime;

public final class SaleTrace {
    private final String saleNumber;
    private final String customerName;
    private final String customerDocument;
    private final LocalDateTime saleDate;
    private final double quantitySold;

    public SaleTrace(
            String saleNumber,
            String customerName,
            String customerDocument,
            LocalDateTime saleDate,
            double quantitySold) {
        this.saleNumber = saleNumber;
        this.customerName = customerName;
        this.customerDocument = customerDocument;
        this.saleDate = saleDate;
        this.quantitySold = quantitySold;
    }

    public String getSaleNumber() {
        return saleNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public double getQuantitySold() {
        return quantitySold;
    }
}
