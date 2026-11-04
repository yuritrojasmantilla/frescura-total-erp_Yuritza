package com.frescuratotal.erp.suppliers.model;

import com.frescuratotal.erp.inventory.model.Product;
import com.frescuratotal.erp.inventory.model.ProductDAO;

import java.sql.*;
import java.util.*;

public final class SupplierDAO {
    private SupplierProfile map(ResultSet r) throws SQLException {
        return new SupplierProfile(
                r.getInt("id"),
                r.getString("name"),
                r.getString("nit"),
                r.getString("contact_person"),
                r.getString("email"),
                r.getString("phone"),
                r.getString("address"),
                r.getBoolean("active"));
    }

    public List<SupplierProfile> findAll(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM suppliers ORDER BY id");
                ResultSet r = s.executeQuery()) {
            List<SupplierProfile> rows = new ArrayList<>();
            while (r.next()) rows.add(map(r));
            return rows;
        }
    }

    public SupplierProfile findById(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM suppliers WHERE id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Supplier not found");
                return map(r);
            }
        }
    }

    public int insert(Connection c, SupplierProfile profile) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO suppliers (name,nit,contact_person,email,phone,address,active)"
                            + " VALUES (?,?,?,?,?,?,TRUE)",
                        Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, profile.getName());
            s.setString(2, profile.getNit());
            s.setString(3, profile.getContactPerson());
            s.setString(4, profile.getEmail());
            s.setString(5, profile.getPhone());
            s.setString(6, profile.getAddress());
            s.executeUpdate();
            return ProductDAO.generatedId(s);
        }
    }

    public void update(Connection c, SupplierProfile profile) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "UPDATE suppliers SET"
                            + " name=?,nit=?,contact_person=?,email=?,phone=?,address=? WHERE id=?"
                            + " AND active=TRUE")) {
            s.setString(1, profile.getName());
            s.setString(2, profile.getNit());
            s.setString(3, profile.getContactPerson());
            s.setString(4, profile.getEmail());
            s.setString(5, profile.getPhone());
            s.setString(6, profile.getAddress());
            s.setInt(7, profile.getId());
            if (s.executeUpdate() != 1)
                throw new IllegalStateException("Active supplier not found");
        }
    }

    public void deactivate(Connection c, int id) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "UPDATE suppliers SET active=FALSE WHERE id=? AND active=TRUE")) {
            s.setInt(1, id);
            if (s.executeUpdate() != 1)
                throw new IllegalStateException("Active supplier not found");
        }
    }

    public List<Product> catalog(Connection c, int supplierId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT p.id,p.name,p.description,p.category_id,p.unit,p.base_price FROM"
                            + " products p JOIN supplier_products sp ON p.id=sp.product_id WHERE"
                            + " sp.supplier_id=? ORDER BY p.name,p.id")) {
            s.setInt(1, supplierId);
            try (ResultSet r = s.executeQuery()) {
                List<Product> rows = new ArrayList<>();
                while (r.next())
                    rows.add(
                            new Product(
                                    r.getInt("id"),
                                    r.getString("name"),
                                    r.getString("description"),
                                    r.getInt("category_id"),
                                    r.getString("unit"),
                                    r.getDouble("base_price")));
                return rows;
            }
        }
    }
}
