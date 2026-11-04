package com.frescuratotal.erp.inventory.model;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class InventoryDAO {
    private static final String LOT_SELECT =
            "SELECT l.*, p.name AS product_name, p.description, p.category_id, p.unit, p.base_price"
                + " FROM inventory_lots l JOIN products p ON p.id=l.product_id ";

    private Lot map(ResultSet r) throws SQLException {
        Product product =
                new Product(
                        r.getInt("product_id"),
                        r.getString("product_name"),
                        r.getString("description"),
                        r.getInt("category_id"),
                        r.getString("unit"),
                        r.getDouble("base_price"));
        return new Lot(
                r.getInt("id"),
                r.getString("lot_number"),
                product,
                r.getInt("supplier_id"),
                r.getDouble("initial_quantity"),
                r.getDouble("quantity"),
                r.getDate("entry_date").toLocalDate(),
                r.getDate("expiration_date").toLocalDate(),
                r.getDouble("purchase_price"),
                r.getDouble("sale_price"),
                r.getString("aisle"),
                r.getString("shelf"),
                r.getInt("status_id"));
    }

    public Lot findById(Connection c, int id, boolean lock) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(LOT_SELECT + "WHERE l.id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Lot not found");
                return map(r);
            }
        }
    }

    public List<Lot> findLots(Connection c, Integer productId, Integer supplierId, String location)
            throws SQLException {
        String sql =
                LOT_SELECT
                        + "WHERE (? IS NULL OR l.product_id=?) AND (? IS NULL OR l.supplier_id=?)"
                        + " AND (? IS NULL OR l.aisle=? OR l.shelf=?) ORDER BY"
                        + " l.expiration_date,l.id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setObject(1, productId);
            s.setObject(2, productId);
            s.setObject(3, supplierId);
            s.setObject(4, supplierId);
            s.setString(5, location);
            s.setString(6, location);
            s.setString(7, location);
            try (ResultSet r = s.executeQuery()) {
                List<Lot> rows = new ArrayList<>();
                while (r.next()) rows.add(map(r));
                return rows;
            }
        }
    }

    public Supplier findSupplier(Connection c, int id) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement("SELECT id,name,nit,email FROM suppliers WHERE id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Supplier not found");
                return new Supplier(
                        r.getInt("id"),
                        r.getString("name"),
                        r.getString("nit"),
                        r.getString("email"));
            }
        }
    }

    public List<Supplier> findSuppliers(Connection c, Integer productId) throws SQLException {
        String sql =
                "SELECT s.id,s.name,s.nit,s.email FROM suppliers s WHERE s.active=TRUE AND (? IS"
                    + " NULL OR EXISTS (SELECT 1 FROM supplier_products sp WHERE"
                    + " sp.supplier_id=s.id AND sp.product_id=?)) ORDER BY s.id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setObject(1, productId);
            s.setObject(2, productId);
            try (ResultSet r = s.executeQuery()) {
                List<Supplier> rows = new ArrayList<>();
                while (r.next())
                    rows.add(
                            new Supplier(
                                    r.getInt("id"),
                                    r.getString("name"),
                                    r.getString("nit"),
                                    r.getString("email")));
                return rows;
            }
        }
    }

    public boolean suppliesProduct(Connection c, int supplierId, int productId)
            throws SQLException {
        String sql =
                "SELECT 1 FROM supplier_products sp JOIN suppliers s ON s.id=sp.supplier_id WHERE"
                    + " sp.supplier_id=? AND sp.product_id=? AND s.active=TRUE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, supplierId);
            s.setInt(2, productId);
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    public int insertLot(Connection c, Lot lot) throws SQLException {
        String sql =
                "INSERT INTO inventory_lots"
                    + " (lot_number,product_id,supplier_id,initial_quantity,quantity,entry_date,expiration_date,purchase_price,sale_price,aisle,shelf,status_id)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, lot.getLotNumber());
            s.setInt(2, lot.getProduct().getId());
            s.setInt(3, lot.getSupplierId());
            s.setDouble(4, lot.getInitialQuantity());
            s.setDouble(5, lot.getQuantity());
            s.setDate(6, java.sql.Date.valueOf(lot.getEntryDate()));
            s.setDate(7, java.sql.Date.valueOf(lot.getExpirationDate()));
            s.setDouble(8, lot.getPurchasePrice());
            s.setDouble(9, lot.getSalePrice());
            s.setString(10, lot.getAisle());
            s.setString(11, lot.getShelf());
            s.setInt(12, lot.getStatusId());
            s.executeUpdate();
            return ProductDAO.generatedId(s);
        }
    }

    public void saveStock(Connection c, Lot lot) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement("UPDATE inventory_lots SET quantity=?,status_id=? WHERE id=?")) {
            s.setDouble(1, lot.getQuantity());
            s.setInt(2, lot.getStatusId());
            s.setInt(3, lot.getId());
            if (s.executeUpdate() != 1) throw new SQLException("Lot could not be updated");
        }
    }

    public int refreshStatuses(Connection c, LocalDate date) throws SQLException {
        String sql =
                "UPDATE inventory_lots SET status_id=CASE WHEN expiration_date<? THEN 3 WHEN"
                    + " expiration_date<=? THEN 2 ELSE 1 END WHERE status_id<>4 AND status_id<>CASE"
                    + " WHEN expiration_date<? THEN 3 WHEN expiration_date<=? THEN 2 ELSE 1 END";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int position : new int[] {1, 3}) s.setDate(position, java.sql.Date.valueOf(date));
            for (int position : new int[] {2, 4})
                s.setDate(position, java.sql.Date.valueOf(date.plusDays(7)));
            return s.executeUpdate();
        }
    }

    public void insertQuarantine(Connection c, int lotId, String reason, String username)
            throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO quarantine_records (lot_id,reason,registered_by) VALUES"
                            + " (?,?,?)")) {
            s.setInt(1, lotId);
            s.setString(2, reason);
            s.setString(3, username);
            s.executeUpdate();
        }
    }

    public boolean hasOpenQuarantine(Connection c, int lotId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT id FROM quarantine_records WHERE lot_id=? AND resolved=FALSE FOR"
                            + " UPDATE")) {
            s.setInt(1, lotId);
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    public void closeQuarantine(Connection c, int lotId, String notes) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "UPDATE quarantine_records SET"
                            + " resolved=TRUE,resolved_at=CURRENT_TIMESTAMP,resolution_notes=?"
                            + " WHERE lot_id=? AND resolved=FALSE")) {
            s.setString(1, notes);
            s.setInt(2, lotId);
            s.executeUpdate();
        }
    }

    public int insertInspection(
            Connection c, int lotId, String result, String inspector, String reason, String notes)
            throws SQLException {
        String sql =
                "INSERT INTO quality_inspections"
                    + " (lot_id,result,inspector_name,rejection_reason,notes) VALUES (?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, lotId);
            s.setString(2, result);
            s.setString(3, inspector);
            s.setString(4, reason);
            s.setString(5, notes);
            s.executeUpdate();
            return ProductDAO.generatedId(s);
        }
    }

    public void insertWaste(
            Connection c,
            int lotId,
            Integer inspectionId,
            double amount,
            String reason,
            String username)
            throws SQLException {
        String sql =
                "INSERT INTO waste_records"
                    + " (lot_id,inspection_id,quantity_lost,reason,registered_by) VALUES"
                    + " (?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, lotId);
            s.setObject(2, inspectionId);
            s.setDouble(3, amount);
            s.setString(4, reason);
            s.setString(5, username);
            s.executeUpdate();
        }
    }

    public List<WasteRecord> findWaste(Connection c, int lotId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT * FROM waste_records WHERE (?=0 OR lot_id=?) ORDER BY id")) {
            s.setInt(1, lotId);
            s.setInt(2, lotId);
            List<WasteRecord> rows = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    int inspection = r.getInt("inspection_id");
                    Integer inspectionId = r.wasNull() ? null : inspection;
                    rows.add(
                            new WasteRecord(
                                    r.getInt("id"),
                                    findById(c, r.getInt("lot_id"), false),
                                    inspectionId,
                                    r.getDouble("quantity_lost"),
                                    r.getString("reason"),
                                    r.getTimestamp("registered_at").toLocalDateTime().toLocalDate(),
                                    r.getString("registered_by")));
                }
            }
            return rows;
        }
    }

    public List<QuarantineRecord> findQuarantines(Connection c, int lotId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT * FROM quarantine_records WHERE (?=0 OR lot_id=?) ORDER BY id")) {
            s.setInt(1, lotId);
            s.setInt(2, lotId);
            List<QuarantineRecord> rows = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next())
                    rows.add(
                            new QuarantineRecord(
                                    r.getInt("id"),
                                    r.getInt("lot_id"),
                                    r.getString("reason"),
                                    r.getString("registered_by"),
                                    r.getTimestamp("registered_at").toLocalDateTime(),
                                    r.getBoolean("resolved"),
                                    r.getString("resolution_notes")));
            }
            return rows;
        }
    }

    public List<QualityInspection> findInspections(Connection c, int lotId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT * FROM quality_inspections WHERE (?=0 OR lot_id=?) ORDER BY id")) {
            s.setInt(1, lotId);
            s.setInt(2, lotId);
            List<QualityInspection> rows = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next())
                    rows.add(
                            new QualityInspection(
                                    r.getInt("id"),
                                    r.getInt("lot_id"),
                                    r.getTimestamp("inspection_date").toLocalDateTime(),
                                    r.getString("result"),
                                    r.getString("inspector_name"),
                                    r.getString("rejection_reason"),
                                    r.getString("notes")));
            }
            return rows;
        }
    }

    public List<SaleTrace> findRecipients(Connection c, int lotId) throws SQLException {
        String sql =
                "SELECT s.sale_number,c.name,c.document,s.sale_date,SUM(d.quantity_sold) AS"
                    + " quantity_sold FROM sale_details d JOIN sales s ON s.id=d.sale_id JOIN"
                    + " customers c ON c.id=s.customer_id WHERE d.lot_id=? GROUP BY"
                    + " s.id,s.sale_number,c.id,c.name,c.document,s.sale_date ORDER BY"
                    + " s.sale_date,s.id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, lotId);
            List<SaleTrace> rows = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next())
                    rows.add(
                            new SaleTrace(
                                    r.getString("sale_number"),
                                    r.getString("name"),
                                    r.getString("document"),
                                    r.getTimestamp("sale_date").toLocalDateTime(),
                                    r.getDouble("quantity_sold")));
            }
            return rows;
        }
    }
}
