package com.frescuratotal.erp.inventory.controller;

import com.frescuratotal.erp.common.ConsoleInput;
import com.frescuratotal.erp.inventory.model.*;
import com.frescuratotal.erp.inventory.view.ProductView;

import java.sql.SQLException;
import java.util.*;

public class ProductController {
    private final ProductView view;
    private final ProductDAO productDAO;

    public ProductController(ProductView view, ProductDAO productDAO) {
        this.view = view;
        this.productDAO = productDAO;
    }

    public void showProduct(Product product) {
        view.showProduct(product);
    }

    public void showProducts(List<Product> products) {
        view.showProducts(products);
    }

    public Product findProductById(List<Product> products, int id) {
        return products.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public void updatePrice(Product product, double newPrice) {
        try {
            if (!productDAO.updatePrice(product.getId(), newPrice))
                throw new IllegalArgumentException("Product no longer exists");
            product.setBasePrice(newPrice);
            view.showProduct(product);
        } catch (SQLException | IllegalArgumentException error) {
            view.showError(error.getMessage());
        }
    }

    public void updateName(Product product, String newName) {
        try {
            if (!productDAO.updateName(product.getId(), newName))
                throw new IllegalArgumentException("Product no longer exists");
            product.setName(newName);
            view.showProduct(product);
        } catch (SQLException | IllegalArgumentException error) {
            view.showError(error.getMessage());
        }
    }

    public void runMenu(Scanner scanner) {
        ConsoleInput input = new ConsoleInput(scanner);
        while (true) {
            view.showMenu();
            try {
                int option = input.integer("Choose an option");
                if (option == 0) return;
                switch (option) {
                    case 1 -> view.showProducts(productDAO.findAll());
                    case 2 -> {
                        Product p =
                                findProductById(productDAO.findAll(), input.integer("Product ID"));
                        updateName(p, input.required("New name"));
                    }
                    case 3 -> {
                        Product p =
                                findProductById(productDAO.findAll(), input.integer("Product ID"));
                        updatePrice(p, input.decimal("New base price"));
                    }
                    case 4 -> {
                        view.showCategories(productDAO.findCategories());
                        String name = input.required("Product name");
                        String description = input.text("Description");
                        int category = input.integer("Existing category ID");
                        String unit = input.required("Unit (kg, unit, liter, box)");
                        double price = input.decimal("Base price");
                        Product p = new Product(0, name, description, category, unit, price);
                        int id = productDAO.insert(p);
                        view.showProduct(new Product(id, name, description, category, unit, price));
                    }
                    case 5 -> view.showCategories(productDAO.findCategories());
                    default -> view.showError("Invalid option");
                }
            } catch (SQLException | IllegalArgumentException error) {
                view.showError(error.getMessage());
            }
        }
    }

    public void runMenu(List<Product> products, Scanner scanner) {
        runMenu(scanner);
    }
}
