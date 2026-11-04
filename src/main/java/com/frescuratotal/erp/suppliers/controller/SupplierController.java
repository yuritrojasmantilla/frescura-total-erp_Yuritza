package com.frescuratotal.erp.suppliers.controller;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.suppliers.model.*;
import com.frescuratotal.erp.suppliers.view.*;

import java.sql.SQLException;
import java.util.Scanner;

public final class SupplierController {
    private final SupplierService service;
    private final SupplierView view;
    private final User user;

    public SupplierController(SupplierService service, SupplierView view, User user) {
        this.service = service;
        this.view = view;
        this.user = user;
    }

    public void runMenu(Scanner scanner) {
        ConsoleInput input = new ConsoleInput(scanner);
        while (true) {
            view.menu();
            try {
                switch (input.integer("Choose an option")) {
                    case 1 -> service.findAll().forEach(view::show);
                    case 2 -> view.show(service.create(user, profile(input, 0)));
                    case 3 -> {
                        int id = input.integer("Supplier ID");
                        view.show(service.findById(id));
                        view.show(service.update(user, profile(input, id)));
                    }
                    case 4 -> {
                        int id = input.integer("Supplier ID to deactivate");
                        view.show(service.findById(id));
                        if ("YES".equals(input.text("Type YES to confirm"))) {
                            service.deactivate(user, id);
                            view.message("Supplier deactivated.");
                        }
                    }
                    case 5 -> view.catalog(service.catalog(input.integer("Supplier ID")));
                    case 6 -> {
                        service.addProduct(
                                user,
                                input.integer("Supplier ID"),
                                input.integer("Existing product ID"));
                        view.message("Product added to supplier catalog.");
                    }
                    case 7 -> view.history(service.purchaseHistory(input.integer("Supplier ID")));
                    case 0 -> {
                        return;
                    }
                    default -> view.message("Invalid option.");
                }
            } catch (SQLException | IllegalArgumentException | IllegalStateException error) {
                view.message("Error: " + error.getMessage());
            }
        }
    }

    private SupplierProfile profile(ConsoleInput input, int id) {
        return new SupplierProfile(
                id,
                input.required("Supplier name"),
                input.required("NIT"),
                input.text("Contact person"),
                input.required("Email"),
                input.text("Phone"),
                input.text("Address"),
                true);
    }
}
