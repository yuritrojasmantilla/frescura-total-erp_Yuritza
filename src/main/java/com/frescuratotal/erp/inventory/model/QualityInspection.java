package com.frescuratotal.erp.inventory.model;

import java.time.LocalDateTime;

public final class QualityInspection {
    private final int id;
    private final int lotId;
    private final LocalDateTime inspectionDate;
    private final String result;
    private final String inspectorName;
    private final String rejectionReason;
    private final String notes;

    public QualityInspection(
            int id,
            int lotId,
            LocalDateTime inspectionDate,
            String result,
            String inspectorName,
            String rejectionReason,
            String notes) {
        this.id = id;
        this.lotId = lotId;
        this.inspectionDate = inspectionDate;
        this.result = result;
        this.inspectorName = inspectorName;
        this.rejectionReason = rejectionReason;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public int getLotId() {
        return lotId;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }

    public String getResult() {
        return result;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getNotes() {
        return notes;
    }
}
