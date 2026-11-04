package com.frescuratotal.erp.sales.model;

import com.frescuratotal.erp.common.Validation;

public final class SaleItemRequest {
    private final int productId;
    private final double quantity, taxRate;

    public SaleItemRequest(int productId, double quantity, double taxRate) {
        this.productId = Validation.positiveId(productId, "Product ID");
        this.quantity = Validation.decimal(quantity, "Quantity", true);
        if (!Double.isFinite(taxRate)
                || taxRate < 0
                || taxRate > 100
                || Math.abs(taxRate * 100 - Math.rint(taxRate * 100)) > 0.000001)
            throw new IllegalArgumentException(
                    "Tax rate must be 0 to 100 with at most two decimals");
        this.taxRate = taxRate;
    }

    public int getProductId() {
        return productId;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getTaxRate() {
        return taxRate;
    }
}
