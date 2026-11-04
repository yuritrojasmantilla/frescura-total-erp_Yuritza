package com.frescuratotal.erp.inventory.model;

import java.time.LocalDateTime;

public final class QuarantineRecord {
    private final int id;
    private final int lotId;
    private final String reason;
    private final String registeredBy;
    private final LocalDateTime registeredAt;
    private final boolean resolved;
    private final String resolutionNotes;

    public QuarantineRecord(
            int id,
            int lotId,
            String reason,
            String registeredBy,
            LocalDateTime registeredAt,
            boolean resolved,
            String resolutionNotes) {
        this.id = id;
        this.lotId = lotId;
        this.reason = reason;
        this.registeredBy = registeredBy;
        this.registeredAt = registeredAt;
        this.resolved = resolved;
        this.resolutionNotes = resolutionNotes;
    }

    public int getId() {
        return id;
    }

    public int getLotId() {
        return lotId;
    }

    public String getReason() {
        return reason;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public boolean getResolved() {
        return resolved;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }
}
