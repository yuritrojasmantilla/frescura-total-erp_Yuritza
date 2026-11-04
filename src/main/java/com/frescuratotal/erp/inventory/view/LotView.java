package com.frescuratotal.erp.inventory.view;

import com.frescuratotal.erp.inventory.model.*;

import java.util.*;

public class LotView {
    public void showMenu() {
        System.out.println("===========================");
        System.out.println("  INVENTORY AND QUALITY    ");
        System.out.println("===========================");
        System.out.println(
                "1. Show all lots\n"
                        + "2. Receive merchandise\n"
                        + "3. Trace a lot\n"
                        + "4. Filter lots\n"
                        + "5. Expiration alerts\n"
                        + "6. Put a lot in quarantine\n"
                        + "7. Inspect a quarantined lot\n"
                        + "8. Register waste\n"
                        + "9. Show waste records\n"
                        + "10. Refresh expiration statuses\n"
                        + "11. Suppliers and purchase history\n"
                        + "12. Available lots in FEFO order\n"
                        + "13. Add product to supplier catalog\n"
                        + "0. Back\n");
    }

    public void showLot(Lot lot) {
        System.out.printf(
                Locale.US,
                "ID: %d%nLot number: %s%nProduct: %s (ID %d)%nSupplier ID: %d%nInitial quantity:"
                    + " %.2f%nCurrent quantity: %.2f%nEntry date: %s%nExpiration date: %s%nPurchase"
                    + " price: %.2f%nSale price: %.2f%nAisle: %s%nShelf: %s%nStatus ID: %d%n%n",
                lot.getId(),
                lot.getLotNumber(),
                lot.getProduct().getName(),
                lot.getProduct().getId(),
                lot.getSupplierId(),
                lot.getInitialQuantity(),
                lot.getQuantity(),
                lot.getEntryDate(),
                lot.getExpirationDate(),
                lot.getPurchasePrice(),
                lot.getSalePrice(),
                lot.getAisle(),
                lot.getShelf(),
                lot.getStatusId());
    }

    public void showLots(List<Lot> lots) {
        if (lots.isEmpty()) System.out.println("No lots found.\n");
        if (!lots.isEmpty())
            System.out.println(
                    "ID | Lot number | Product | Supplier | Quantity | Expiration | Aisle/Shelf |"
                            + " Status ID");
        for (Lot lot : lots)
            System.out.printf(
                    Locale.US,
                    "%d | %s | %s | %d | %.2f | %s | %s/%s | %d%n",
                    lot.getId(),
                    lot.getLotNumber(),
                    lot.getProduct().getName(),
                    lot.getSupplierId(),
                    lot.getQuantity(),
                    lot.getExpirationDate(),
                    lot.getAisle(),
                    lot.getShelf(),
                    lot.getStatusId());
        System.out.println();
    }

    public void showSupplier(Supplier supplier) {
        System.out.println(
                "Supplier ID: "
                        + supplier.getId()
                        + " | "
                        + supplier.getName()
                        + " | NIT: "
                        + supplier.getNit()
                        + " | "
                        + supplier.getEmail());
    }

    public void showWasteRecord(WasteRecord record) {
        System.out.printf(
                Locale.US,
                "Waste ID: %d | Lot: %s | Inspection ID: %s%nQuantity lost: %.2f | Date: %s | User:"
                        + " %s%nReason: %s%n%n",
                record.getId(),
                record.getLot().getLotNumber(),
                record.getInspectionId(),
                record.getQuantityLost(),
                record.getDate(),
                record.getRegisteredBy(),
                record.getReason());
    }

    public void showQuarantine(QuarantineRecord record) {
        System.out.println(
                "Quarantine ID: "
                        + record.getId()
                        + " | Lot ID: "
                        + record.getLotId()
                        + " | Resolved: "
                        + record.getResolved()
                        + " | User: "
                        + record.getRegisteredBy());
        System.out.println(
                "Date: "
                        + record.getRegisteredAt().toLocalDate()
                        + " | Reason: "
                        + record.getReason()
                        + " | Resolution: "
                        + Objects.toString(record.getResolutionNotes(), "Pending")
                        + "\n");
    }

    public void showInspection(QualityInspection inspection) {
        System.out.println(
                "Inspection ID: "
                        + inspection.getId()
                        + " | Lot ID: "
                        + inspection.getLotId()
                        + " | Result: "
                        + inspection.getResult()
                        + " | Inspector: "
                        + inspection.getInspectorName());
        System.out.println(
                "Date: "
                        + inspection.getInspectionDate().toLocalDate()
                        + " | Rejection reason: "
                        + Objects.toString(inspection.getRejectionReason(), "None")
                        + " | Notes: "
                        + inspection.getNotes()
                        + "\n");
    }

    public void showRecipient(SaleTrace trace) {
        System.out.printf(
                Locale.US,
                "Sale: %s | Customer: %s | Document: %s | Date: %s | Quantity: %.2f%n",
                trace.getSaleNumber(),
                trace.getCustomerName(),
                trace.getCustomerDocument(),
                trace.getSaleDate().toLocalDate(),
                trace.getQuantitySold());
    }

    public void showError(String message) {
        System.out.println("Error: " + message + "\n");
    }

    public void showMessage(String message) {
        System.out.println(message + "\n");
    }
}
