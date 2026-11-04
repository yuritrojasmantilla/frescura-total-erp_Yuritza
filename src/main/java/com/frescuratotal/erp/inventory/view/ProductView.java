package com.frescuratotal.erp.inventory.view;

import com.frescuratotal.erp.inventory.model.*;

import java.util.*;

public class ProductView {
    public void showMenu() {
        System.out.println("===========================");
        System.out.println("         PRODUCTS          ");
        System.out.println("===========================");
        System.out.println(
                "1. Show products\n"
                        + "2. Update name\n"
                        + "3. Update price\n"
                        + "4. Create product\n"
                        + "5. Show categories\n"
                        + "0. Back\n");
    }

    public void showProduct(Product product) {
        System.out.printf(
                Locale.US,
                "ID: %d | Product: %s | Category ID: %d | Unit: %s | Base price: %.2f%nDescription:"
                        + " %s%n%n",
                product.getId(),
                product.getName(),
                product.getCategoryId(),
                product.getUnit(),
                product.getBasePrice(),
                product.getDescription());
    }

    public void showProducts(List<Product> products) {
        if (products.isEmpty()) System.out.println("No products found.\n");
        for (Product product : products) showProduct(product);
    }

    public void showCategories(List<ProductCategory> categories) {
        for (ProductCategory category : categories)
            System.out.println(category.getId() + " | " + category.getName());
        System.out.println();
    }

    public void showError(String message) {
        System.out.println("Error: " + message + "\n");
    }
}
