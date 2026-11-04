package com.frescuratotal.erp.suppliers.view;

import com.frescuratotal.erp.inventory.model.*;
import com.frescuratotal.erp.suppliers.model.SupplierProfile;

import java.util.*;

public final class SupplierView {
    public void menu() {
        System.out.println("===========================");
        System.out.println("  SUPPLIERS AND PURCHASES  ");
        System.out.println("===========================");
        System.out.println(
                "1. Directory\n2. Register supplier"
                        + "\n3. Edit supplier\n4. Deactivate supplier\n5. Supplier catalog"
                        + "\n6. Add catalog product\n7. Purchase / receipt history\n0. Back\n");
    }

    public void show(SupplierProfile s) {
        System.out.println(
                "Supplier "
                        + s.getId()
                        + " | "
                        + s.getName()
                        + " | NIT "
                        + s.getNit()
                        + " | "
                        + (s.isActive() ? "Active" : "Inactive"));
        System.out.println(
                "Contact: "
                        + s.getContactPerson()
                        + " | "
                        + s.getEmail()
                        + " | "
                        + s.getPhone()
                        + " | "
                        + s.getAddress());
    }

    public void catalog(List<Product> products) {
        if (products.isEmpty()) System.out.println("Supplier catalog is empty.");
        for (Product p : products)
            System.out.println(p.getId() + " | " + p.getName() + " | " + p.getUnit());
    }

    public void history(List<Lot> lots) {
        if (lots.isEmpty()) System.out.println("No receipts recorded.");
        for (Lot lot : lots)
            System.out.printf(
                    Locale.US,
                    "%s | %s | %s | received %.2f %s | remaining %.2f | expiry %s%n",
                    lot.getLotNumber(),
                    lot.getEntryDate(),
                    lot.getProduct().getName(),
                    lot.getInitialQuantity(),
                    lot.getProduct().getUnit(),
                    lot.getQuantity(),
                    lot.getExpirationDate());
    }

    public void message(String text) {
        System.out.println(text);
    }
}
