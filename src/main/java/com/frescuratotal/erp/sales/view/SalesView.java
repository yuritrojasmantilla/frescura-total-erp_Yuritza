package com.frescuratotal.erp.sales.view;

import com.frescuratotal.erp.inventory.model.Lot;
import com.frescuratotal.erp.sales.model.*;

import java.util.*;

public final class SalesView {
    public void menu() {
        System.out.println("===========================");
        System.out.println("     SALES AND BILLING     ");
        System.out.println("===========================");
        System.out.println(
                "1. Customers\n"
                    + "2. Register customer\n"
                    + "3. Sell using FEFO\n"
                    + "4. Sale details\n"
                    + "5. Recent sales\n"
                    + "6. Export simulated invoice PNG\n"
                    + "7. Sellable lots for product\n"
                    + "0. Back\n");
    }

    public void customer(Customer c) {
        System.out.println(
                "Customer "
                        + c.getId()
                        + " | "
                        + c.getName()
                        + " | document "
                        + c.getDocument()
                        + " | "
                        + (c.getTypeId() == 1 ? "Wholesale" : "Retail")
                        + " | "
                        + c.getEmail());
    }

    public void lots(List<Lot> lots) {
        if (lots.isEmpty()) message("No sellable lots.");
        for (Lot lot : lots)
            System.out.printf(
                    Locale.US,
                    "Lot %d | %s | %s | available %.2f %s | expiry %s | price %.2f | status %d%s%n",
                    lot.getId(),
                    lot.getLotNumber(),
                    lot.getProduct().getName(),
                    lot.getQuantity(),
                    lot.getProduct().getUnit(),
                    lot.getExpirationDate(),
                    lot.getSalePrice(),
                    lot.getStatusId(),
                    lot.getStatusId() == Lot.NEAR_EXPIRY ? " | 50% promotion" : "");
    }

    public void sale(Sale sale) {
        System.out.printf(
                Locale.US,
                "Sale %d | %s | %s | %s | customer %s%n",
                sale.getId(),
                sale.getNumber(),
                sale.getDate(),
                sale.getStatus(),
                sale.getCustomer().getName());
        for (SaleLine line : sale.getLines())
            System.out.printf(
                    Locale.US,
                    "  %s | lot %s | %.2f %s x %.2f | discount %.2f%% = %.2f"
                            + " | tax %.2f%% = %.2f | total %.2f%n",
                    line.getProductName(),
                    line.getLotNumber(),
                    line.getQuantity(),
                    line.getUnit(),
                    line.getUnitPrice(),
                    line.getDiscountRate(),
                    line.getDiscountAmount(),
                    line.getTaxRate(),
                    line.getTaxAmount(),
                    line.getLineTotal());
        System.out.printf(
                Locale.US,
                "Subtotal %.2f | discount %.2f | tax %.2f | TOTAL %.2f%n",
                sale.getSubtotal(),
                sale.getDiscount(),
                sale.getTax(),
                sale.getTotal());
        if (sale.getSimulatedCufe() != null) message("Simulated CUFE: " + sale.getSimulatedCufe());
    }

    public void message(String value) {
        System.out.println(value);
    }
}
