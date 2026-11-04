package com.frescuratotal.erp.suppliers.model;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.inventory.model.*;

import java.sql.*;
import java.util.*;

public final class SupplierService {
    private final ConnectionProvider connections;
    private final SupplierDAO dao = new SupplierDAO();
    private final InventoryService inventory;

    public SupplierService(InventoryService inventory) {
        this(DatabaseConnection::getConnection, inventory);
    }

    public SupplierService(ConnectionProvider connections, InventoryService inventory) {
        this.connections = connections;
        this.inventory = inventory;
    }

    private void requireUser(Connection c, User user) throws SQLException {
        if (user == null) throw new IllegalStateException("Login required");
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT 1 FROM app_users WHERE id=? AND username=? AND active=TRUE")) {
            s.setInt(1, user.getId());
            s.setString(2, user.getUsername());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("User is inactive");
            }
        }
    }

    public List<SupplierProfile> findAll() throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findAll(c);
        }
    }

    public SupplierProfile findById(int id) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findById(c, id);
        }
    }

    public List<Product> catalog(int id) throws SQLException {
        try (Connection c = connections.open()) {
            dao.findById(c, id);
            return dao.catalog(c, id);
        }
    }

    public List<Lot> purchaseHistory(int id) throws SQLException {
        findById(id);
        return inventory.findLots(null, id, null);
    }

    public void addProduct(User user, int supplierId, int productId) throws SQLException {
        inventory.linkSupplierProduct(user, supplierId, productId);
        AppLogger.info("Supplier " + supplierId + " linked to product " + productId);
    }

    public SupplierProfile create(User user, SupplierProfile profile) throws SQLException {
        try (Connection c = connections.open()) {
            requireUser(c, user);
            int id = dao.insert(c, profile);
            AppLogger.info("Supplier registered: ID " + id);
            return dao.findById(c, id);
        }
    }

    public SupplierProfile update(User user, SupplierProfile profile) throws SQLException {
        try (Connection c = connections.open()) {
            requireUser(c, user);
            dao.update(c, profile);
            return dao.findById(c, profile.getId());
        }
    }

    public void deactivate(User user, int id) throws SQLException {
        try (Connection c = connections.open()) {
            requireUser(c, user);
            dao.deactivate(c, id);
        }
    }
}
