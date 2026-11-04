package com.frescuratotal.erp.sales.controller;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.sales.model.*;
import com.frescuratotal.erp.sales.view.*;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

public final class SalesController {
    private final SalesService service;
    private final SalesView view;
    private final User user;

    public SalesController(SalesService service, SalesView view, User user) {
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
                    case 1 -> service.customers().forEach(view::customer);
                    case 2 -> {
                        String name = input.required("Customer name");
                        String doc = input.required("NIT or document");
                        String email = input.text("Email");
                        String phone = input.text("Phone");
                        String address = input.text("Address");
                        int type = input.integer("Type: 1 Wholesale, 2 Retail");
                        view.customer(
                                service.registerCustomer(
                                        user,
                                        new Customer(0, name, doc, email, phone, address, type)));
                    }
                    case 3 -> createSale(input);
                    case 4 -> view.sale(service.findSale(input.integer("Sale ID")));
                    case 5 -> service.recent(10).forEach(view::sale);
                    case 6 -> export(input.integer("Sale ID"));
                    case 7 -> view.lots(service.sellable(input.integer("Product ID")));
                    case 0 -> {
                        return;
                    }
                    default -> view.message("Invalid option.");
                }
            } catch (SQLException
                    | IOException
                    | IllegalArgumentException
                    | IllegalStateException error) {
                view.message("Error: " + error.getMessage());
            }
        }
    }

    private void createSale(ConsoleInput input) throws SQLException, IOException {
        service.customers().forEach(view::customer);
        int customerId = input.integer("Customer ID");
        List<SaleItemRequest> order = new ArrayList<>();
        while (true) {
            int id = input.integer("Product ID (0 when finished)");
            if (id == 0) break;
            view.lots(service.sellable(id));
            double quantity = input.decimal("Quantity requested");
            double rate = input.decimal("Tax rate for this product (%)");
            order.add(new SaleItemRequest(id, quantity, rate));
        }
        if (order.isEmpty()) {
            view.message("Order cancelled: no items.");
            return;
        }
        view.message("The applicable tax rate is supplied by you; no legal rate is assumed.");
        String payment = input.required("Payment: CASH, CARD or TRANSFER");
        if (!"YES".equals(input.text("Type YES to confirm sale and deduct stock"))) {
            view.message("Order cancelled.");
            return;
        }
        Sale sale = service.confirm(user, customerId, order, payment);
        view.sale(sale);
        try {
            export(sale.getId());
        } catch (IOException error) {
            view.message(
                    "Sale and PNG are safely saved in MySQL. Export later from option 6: "
                            + error.getMessage());
        }
    }

    private void export(int saleId) throws SQLException, IOException {
        Path result = service.exportInvoice(saleId, Path.of("invoices"));
        view.message("Academic invoice PNG saved: " + result);
    }
}
