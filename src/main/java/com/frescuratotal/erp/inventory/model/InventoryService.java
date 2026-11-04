package com.frescuratotal.erp.inventory.model;

import com.frescuratotal.erp.common.*;

import java.sql.*;
import java.time.*;
import java.util.*;

public class InventoryService {
    private final ConnectionProvider connections;
    private final Clock clock;
    private final InventoryDAO dao = new InventoryDAO();
    private final ProductDAO products;
    private LocalDate lastRefresh;

    public InventoryService() {
        this(DatabaseConnection::getConnection, Clock.systemDefaultZone());
    }

    public InventoryService(ConnectionProvider connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
        this.products = new ProductDAO(connections);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private <T> T transaction(Work<T> work) throws SQLException {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                try {
                    c.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    private void requireUser(Connection c, User user, boolean inspectorOnly) throws SQLException {
        if (user == null) throw new IllegalStateException("Login is required");
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT role FROM app_users WHERE id=? AND username=? AND active=TRUE")) {
            s.setInt(1, user.getId());
            s.setString(2, user.getUsername());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next())
                    throw new IllegalStateException("User is inactive or no longer exists");
                if (inspectorOnly && !"INSPECTOR".equals(r.getString("role")))
                    throw new IllegalStateException("Only an inspector can resolve an inspection");
            }
        }
    }

    public synchronized int refreshIfNewDay() throws SQLException {
        LocalDate date = today();
        if (date.equals(lastRefresh)) return 0;
        int updated = refreshStatuses();
        lastRefresh = date;
        return updated;
    }

    public int refreshStatuses() throws SQLException {
        try (Connection c = connections.open()) {
            return dao.refreshStatuses(c, today());
        }
    }

    public List<Lot> findLots(Integer productId, Integer supplierId, String location)
            throws SQLException {
        refreshIfNewDay();
        try (Connection c = connections.open()) {
            return dao.findLots(c, productId, supplierId, location);
        }
    }

    public Lot findLot(int id) throws SQLException {
        refreshIfNewDay();
        try (Connection c = connections.open()) {
            return dao.findById(c, id, false);
        }
    }

    public List<Lot> findSellableLots(int productId) throws SQLException {
        return findLots(productId, null, null).stream()
                .filter(lot -> lot.isSellable(today()))
                .toList();
    }

    public List<Supplier> findSuppliers(Integer productId) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findSuppliers(c, productId);
        }
    }

    public void linkSupplierProduct(User user, int supplierId, int productId) throws SQLException {
        transaction(
                c -> {
                    requireUser(c, user, false);
                    products.findById(c, productId);
                    boolean active =
                            dao.findSuppliers(c, null).stream()
                                    .anyMatch(s -> s.getId() == supplierId);
                    if (!active) throw new IllegalArgumentException("Active supplier not found");
                    if (dao.suppliesProduct(c, supplierId, productId))
                        throw new IllegalArgumentException("Supplier already lists this product");
                    try (PreparedStatement statement =
                            c.prepareStatement(
                                    "INSERT INTO supplier_products (supplier_id,product_id) VALUES"
                                            + " (?,?)")) {
                        statement.setInt(1, supplierId);
                        statement.setInt(2, productId);
                        statement.executeUpdate();
                    }
                    return null;
                });
    }

    public Supplier findSupplier(int id) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findSupplier(c, id);
        }
    }

    public Lot receive(
            User user,
            int productId,
            int supplierId,
            double amount,
            LocalDate entryDate,
            LocalDate expirationDate,
            double purchasePrice,
            double salePrice,
            String aisle,
            String shelf)
            throws SQLException {
        if (entryDate == null || entryDate.isAfter(today()))
            throw new IllegalArgumentException("Entry date cannot be in the future");
        Lot received = transaction(
                c -> {
                    requireUser(c, user, false);
                    Product product = products.findById(c, productId);
                    if (!dao.suppliesProduct(c, supplierId, productId))
                        throw new IllegalArgumentException(
                                "The active supplier must list this product in its catalog");
                    Lot lot =
                            new Lot(
                                    0,
                                    "LOT-" + UUID.randomUUID(),
                                    product,
                                    supplierId,
                                    amount,
                                    amount,
                                    entryDate,
                                    expirationDate,
                                    purchasePrice,
                                    salePrice,
                                    aisle,
                                    shelf,
                                    Lot.AVAILABLE);
                    lot.updateStatus(today());
                    int id = dao.insertLot(c, lot);
                    return dao.findById(c, id, false);
                });
        AppLogger.info("Received lot " + received.getLotNumber() + " with product ID " + productId);
        return received;
    }

    public Lot putInQuarantine(User user, int lotId, String reason) throws SQLException {
        String clean = Validation.text(reason, "Quarantine reason", 2000);
        Lot quarantined = transaction(
                c -> {
                    requireUser(c, user, false);
                    Lot lot = dao.findById(c, lotId, true);
                    if (lot.getQuantity() == 0)
                        throw new IllegalStateException("An empty lot cannot enter quarantine");
                    if (lot.getStatusId() == Lot.IN_QUARANTINE || dao.hasOpenQuarantine(c, lotId))
                        throw new IllegalStateException("Lot already has an open quarantine");
                    lot.putInQuarantine();
                    dao.saveStock(c, lot);
                    dao.insertQuarantine(c, lotId, clean, user.getUsername());
                    return lot;
                });
        AppLogger.info("Lot " + lotId + " quarantined by " + user.getUsername());
        return quarantined;
    }

    public Lot inspect(User user, int lotId, boolean approved, String rejectionReason, String notes)
            throws SQLException {
        String cleanNotes = Validation.text(notes, "Inspection notes", 2000);
        String cleanReason =
                approved ? null : Validation.text(rejectionReason, "Rejection reason", 2000);
        Lot inspected = transaction(
                c -> {
                    requireUser(c, user, true);
                    Lot lot = dao.findById(c, lotId, true);
                    if (lot.getStatusId() != Lot.IN_QUARANTINE || !dao.hasOpenQuarantine(c, lotId))
                        throw new IllegalStateException("Lot has no pending quarantine inspection");
                    if (lot.getQuantity() <= 0)
                        throw new IllegalStateException("An empty lot cannot be inspected");
                    int inspectionId =
                            dao.insertInspection(
                                    c,
                                    lotId,
                                    approved ? "Approved" : "Rejected",
                                    user.getUsername(),
                                    cleanReason,
                                    cleanNotes);
                    if (approved) lot.approveInspection(today());
                    else {
                        double amount = lot.getQuantity();
                        lot.removeQuantity(amount);
                        dao.insertWaste(
                                c, lotId, inspectionId, amount, cleanReason, user.getUsername());
                    }
                    dao.saveStock(c, lot);
                    dao.closeQuarantine(
                            c, lotId, (approved ? "Approved: " : "Rejected: ") + cleanNotes);
                    return lot;
                });
        AppLogger.info("Lot " + lotId + " inspection " + (approved ? "approved" : "rejected")
                + " by " + user.getUsername());
        return inspected;
    }

    public Lot registerWaste(User user, int lotId, double amount, String reason)
            throws SQLException {
        String clean = Validation.text(reason, "Waste reason", 2000);
        Lot updated = transaction(
                c -> {
                    requireUser(c, user, false);
                    Lot lot = dao.findById(c, lotId, true);
                    lot.updateStatus(today());
                    if (lot.getStatusId() == Lot.IN_QUARANTINE) {
                        throw new IllegalStateException(
                                "Quarantined lots require an inspector decision through the"
                                    + " inspection menu");
                    }
                    lot.removeQuantity(amount);
                    dao.insertWaste(c, lotId, null, amount, clean, user.getUsername());
                    dao.saveStock(c, lot);
                    return lot;
                });
        AppLogger.info("Waste registered: lot " + lotId + ", quantity " + amount);
        return updated;
    }

    public List<WasteRecord> findWaste(int lotId) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findWaste(c, lotId);
        }
    }

    public List<QuarantineRecord> findQuarantines(int lotId) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findQuarantines(c, lotId);
        }
    }

    public List<QualityInspection> findInspections(int lotId) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findInspections(c, lotId);
        }
    }

    public List<SaleTrace> findRecipients(int lotId) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findRecipients(c, lotId);
        }
    }
}
