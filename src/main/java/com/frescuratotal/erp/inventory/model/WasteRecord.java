package com.frescuratotal.erp.inventory.model;

import com.frescuratotal.erp.common.Validation;

import java.time.LocalDate;

public final class WasteRecord {
    private final int id;
    private final Lot lot;
    private final Integer inspectionId;
    private final double quantityLost;
    private final String reason;
    private final LocalDate date;
    private final String registeredBy;

    public WasteRecord(
            int id,
            Lot lot,
            Integer inspectionId,
            double quantityLost,
            String reason,
            LocalDate date,
            String registeredBy) {
        if (lot == null || date == null)
            throw new IllegalArgumentException("Lot and date are required");
        if (inspectionId != null && inspectionId <= 0)
            throw new IllegalArgumentException("Inspection ID must be positive");
        Validation.decimal(quantityLost, "Quantity lost", true);
        Validation.text(reason, "Reason", 2000);
        Validation.text(registeredBy, "Registered by", 100);
        this.id = id;
        this.lot = lot;
        this.inspectionId = inspectionId;
        this.quantityLost = quantityLost;
        this.reason = reason;
        this.date = date;
        this.registeredBy = registeredBy;
    }

    public int getId() {
        return id;
    }

    public Lot getLot() {
        return lot;
    }

    public Integer getInspectionId() {
        return inspectionId;
    }

    public double getQuantityLost() {
        return quantityLost;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }
}
