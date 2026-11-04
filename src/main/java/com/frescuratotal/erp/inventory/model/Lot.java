package com.frescuratotal.erp.inventory.model;

import com.frescuratotal.erp.common.Validation;

import java.time.LocalDate;

public class Lot {
    public static final int AVAILABLE = 1;
    public static final int NEAR_EXPIRY = 2;
    public static final int EXPIRED = 3;
    public static final int IN_QUARANTINE = 4;
    private final int id;
    private final String lotNumber;
    private final Product product;
    private final int supplierId;
    private final double initialQuantity;
    private double quantity;
    private final LocalDate entryDate;
    private final LocalDate expirationDate;
    private final double purchasePrice;
    private final double salePrice;
    private final String aisle;
    private final String shelf;
    private int statusId;

    public Lot(
            int id,
            String lotNumber,
            Product product,
            int supplierId,
            double initialQuantity,
            double quantity,
            LocalDate entryDate,
            LocalDate expirationDate,
            double purchasePrice,
            double salePrice,
            String aisle,
            String shelf,
            int statusId) {
        if (id < 0) throw new IllegalArgumentException("Lot ID cannot be negative");
        if (product == null) throw new IllegalArgumentException("Product is required");
        if (entryDate == null || expirationDate == null)
            throw new IllegalArgumentException("Entry and expiration dates are required");
        if (expirationDate.isBefore(entryDate))
            throw new IllegalArgumentException("Expiration date cannot be before entry date");
        if (statusId < AVAILABLE || statusId > IN_QUARANTINE)
            throw new IllegalArgumentException("Unknown lot status");
        this.id = id;
        this.lotNumber = Validation.text(lotNumber, "Lot number", 50);
        this.product = product;
        this.supplierId = Validation.positiveId(supplierId, "Supplier ID");
        this.initialQuantity = Validation.decimal(initialQuantity, "Initial quantity", true);
        this.quantity = Validation.decimal(quantity, "Quantity", false);
        if (quantity > initialQuantity)
            throw new IllegalArgumentException("Quantity cannot exceed initial quantity");
        this.entryDate = entryDate;
        this.expirationDate = expirationDate;
        this.purchasePrice = Validation.decimal(purchasePrice, "Purchase price", false);
        this.salePrice = Validation.decimal(salePrice, "Sale price", false);
        this.aisle = Validation.text(aisle, "Aisle", 50);
        this.shelf = Validation.text(shelf, "Shelf", 50);
        this.statusId = statusId;
    }

    public int getId() {
        return id;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public Product getProduct() {
        return product;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public double getInitialQuantity() {
        return initialQuantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public String getAisle() {
        return aisle;
    }

    public String getShelf() {
        return shelf;
    }

    public int getStatusId() {
        return statusId;
    }

    private void requireDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Reference date is required");
    }

    public boolean isExpired(LocalDate date) {
        requireDate(date);
        return expirationDate.isBefore(date);
    }

    public boolean isNearExpiry(LocalDate date) {
        requireDate(date);
        return !isExpired(date) && !expirationDate.isAfter(date.plusDays(7));
    }

    public void updateStatus(LocalDate date) {
        requireDate(date);
        if (statusId == IN_QUARANTINE) return;
        statusId = isExpired(date) ? EXPIRED : isNearExpiry(date) ? NEAR_EXPIRY : AVAILABLE;
    }

    public boolean isSellable(LocalDate date) {
        requireDate(date);
        return (statusId == AVAILABLE || statusId == NEAR_EXPIRY)
                && !isExpired(date)
                && !date.isBefore(entryDate)
                && quantity > 0;
    }

    public void removeQuantity(double amount) {
        amount = Validation.decimal(amount, "Amount", true);
        if (amount > quantity) throw new IllegalArgumentException("Insufficient stock");
        quantity = (Math.round(quantity * 100) - Math.round(amount * 100)) / 100.0;
    }

    public void removeForSale(double amount, LocalDate date) {
        if (!isSellable(date)) throw new IllegalStateException("Lot is not available for sale");
        removeQuantity(amount);
    }

    public void putInQuarantine() {
        statusId = IN_QUARANTINE;
    }

    public void approveInspection(LocalDate date) {
        requireDate(date);
        if (statusId != IN_QUARANTINE) throw new IllegalStateException("Lot is not in quarantine");
        statusId = AVAILABLE;
        updateStatus(date);
    }
}
