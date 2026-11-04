package com.frescuratotal.erp.inventory.controller;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.inventory.model.*;
import com.frescuratotal.erp.inventory.view.LotView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class LotController {
    private final LotView view;
    private final InventoryService service;
    private final User user;

    public LotController(LotView view, InventoryService service, User user) {
        this.view = view;
        this.service = service;
        this.user = user;
    }

    public void runMenu(Scanner scanner) {
        ConsoleInput input = new ConsoleInput(scanner);
        while (true) {
            view.showMenu();
            try {
                service.refreshIfNewDay();
                int option = input.integer("Choose an option");
                if (option == 0) return;
                switch (option) {
                    case 1 -> view.showLots(service.findLots(null, null, null));
                    case 2 -> receive(input);
                    case 3 -> trace(input.integer("Lot ID"));
                    case 4 -> {
                        int productId = input.integer("Product ID (0 for any)");
                        int supplierId = input.integer("Supplier ID (0 for any)");
                        String location = input.text("Aisle or shelf (empty for any)");
                        view.showLots(
                                service.findLots(
                                        productId == 0 ? null : productId,
                                        supplierId == 0 ? null : supplierId,
                                        location.isBlank() ? null : location));
                    }
                    case 5 -> alerts();
                    case 6 ->
                            view.showLot(
                                    service.putInQuarantine(
                                            user,
                                            input.integer("Lot ID"),
                                            input.required("Quarantine reason")));
                    case 7 -> inspect(input);
                    case 8 ->
                            view.showLot(
                                    service.registerWaste(
                                            user,
                                            input.integer("Lot ID"),
                                            input.decimal("Quantity lost"),
                                            input.required("Waste reason")));
                    case 9 -> {
                        List<WasteRecord> rows =
                                service.findWaste(input.integer("Lot ID (0 for all)"));
                        if (rows.isEmpty()) view.showMessage("No waste records found.");
                        for (WasteRecord record : rows) view.showWasteRecord(record);
                    }
                    case 10 -> view.showMessage("Statuses updated: " + service.refreshStatuses());
                    case 11 -> {
                        for (Supplier supplier : service.findSuppliers(null))
                            view.showSupplier(supplier);
                        int supplierId =
                                input.integer("Supplier ID for delivery history (0 to return)");
                        if (supplierId != 0) {
                            view.showSupplier(service.findSupplier(supplierId));
                            view.showLots(service.findLots(null, supplierId, null));
                        }
                    }
                    case 12 -> {
                        List<Lot> lots = service.findSellableLots(input.integer("Product ID"));
                        view.showLots(lots);
                        double total = lots.stream().mapToDouble(Lot::getQuantity).sum();
                        view.showMessage(String.format(Locale.US, "Sellable stock: %.2f", total));
                    }
                    case 13 -> {
                        for (Supplier supplier : service.findSuppliers(null))
                            view.showSupplier(supplier);
                        service.linkSupplierProduct(
                                user,
                                input.integer("Supplier ID"),
                                input.integer("Existing product ID"));
                        view.showMessage("Product added to supplier catalog.");
                    }
                    default -> view.showError("Invalid option");
                }
            } catch (SQLException | IllegalArgumentException | IllegalStateException error) {
                view.showError(error.getMessage());
            }
        }
    }

    private void receive(ConsoleInput input) throws SQLException {
        int productId = input.integer("Existing product ID");
        List<Supplier> suppliers = service.findSuppliers(productId);
        if (suppliers.isEmpty())
            throw new IllegalArgumentException(
                    "No active supplier lists this product. Configure its supplier catalog first.");
        for (Supplier supplier : suppliers) view.showSupplier(supplier);
        int supplierId = input.integer("Supplier ID from this catalog");
        double amount = input.decimal("Quantity received");
        LocalDate entryDate = input.date("Entry date");
        LocalDate expirationDate = input.date("Expiration date");
        double purchasePrice = input.decimal("Purchase price per unit");
        double salePrice = input.decimal("Sale price per unit");
        String aisle = input.required("Aisle");
        String shelf = input.required("Shelf");
        Lot lot =
                service.receive(
                        user,
                        productId,
                        supplierId,
                        amount,
                        entryDate,
                        expirationDate,
                        purchasePrice,
                        salePrice,
                        aisle,
                        shelf);
        view.showMessage("Receipt saved. The lot number was generated automatically.");
        view.showLot(lot);
    }

    private void inspect(ConsoleInput input) throws SQLException {
        if (!user.isInspector())
            throw new IllegalStateException("Only an inspector can resolve an inspection");
        int lotId = input.integer("Lot ID");
        int result = input.integer("Inspection result: 1 Approved, 2 Rejected");
        if (result != 1 && result != 2)
            throw new IllegalArgumentException("Invalid inspection result");
        String reason = result == 2 ? input.required("Rejection reason") : null;
        String notes = input.required("Inspection notes");
        view.showLot(service.inspect(user, lotId, result == 1, reason, notes));
    }

    public void alerts() throws SQLException {
        List<Lot> lots = service.findLots(null, null, null);
        List<Lot> near =
                lots.stream()
                        .filter(
                                l ->
                                        l.getQuantity() > 0
                                                && l.getStatusId() != Lot.IN_QUARANTINE
                                                && l.isNearExpiry(service.today()))
                        .toList();
        List<Lot> expired =
                lots.stream()
                        .filter(l -> l.getQuantity() > 0 && l.isExpired(service.today()))
                        .toList();
        view.showMessage("Near-expiry lots with stock: " + near.size());
        view.showLots(near);
        view.showMessage(
                "Expired lots with stock: "
                        + expired.size()
                        + ". Blocked for sale; record a waste write-off.");
        view.showLots(expired);
    }

    private void trace(int lotId) throws SQLException {
        Lot lot = service.findLot(lotId);
        view.showLot(lot);
        view.showSupplier(service.findSupplier(lot.getSupplierId()));
        view.showMessage("CUSTOMERS WHO RECEIVED THIS LOT");
        List<SaleTrace> recipients = service.findRecipients(lotId);
        if (recipients.isEmpty()) view.showMessage("No recorded sales for this lot.");
        for (SaleTrace trace : recipients) view.showRecipient(trace);
        view.showMessage("QUARANTINE HISTORY");
        for (QuarantineRecord q : service.findQuarantines(lotId)) view.showQuarantine(q);
        view.showMessage("INSPECTION HISTORY");
        for (QualityInspection inspection : service.findInspections(lotId))
            view.showInspection(inspection);
        view.showMessage("WASTE HISTORY");
        for (WasteRecord waste : service.findWaste(lotId)) view.showWasteRecord(waste);
    }
}
