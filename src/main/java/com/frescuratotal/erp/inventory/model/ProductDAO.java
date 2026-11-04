package com.frescuratotal.erp.inventory.model;

import com.frescuratotal.erp.common.*;

import java.sql.*;
import java.util.*;

public class ProductDAO {
    private final ConnectionProvider connections;

    public ProductDAO() {
        this(DatabaseConnection::getConnection);
    }

    public ProductDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    public List<Product> findAll() throws SQLException {
        try (Connection c = connections.open();
                PreparedStatement s = c.prepareStatement("SELECT * FROM products ORDER BY id");
                ResultSet r = s.executeQuery()) {
            List<Product> result = new ArrayList<>();
            while (r.next()) result.add(map(r));
            return result;
        }
    }

    public Product findById(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM products WHERE id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Product not found");
                return map(r);
            }
        }
    }

    private Product map(ResultSet r) throws SQLException {
        return new Product(
                r.getInt("id"),
                r.getString("name"),
                r.getString("description"),
                r.getInt("category_id"),
                r.getString("unit"),
                r.getDouble("base_price"));
    }

    public int insert(Product product) throws SQLException {
        String sql =
                "INSERT INTO products (name,description,category_id,unit,base_price) VALUES"
                    + " (?,?,?,?,?)";
        try (Connection c = connections.open();
                PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, product.getName());
            s.setString(2, product.getDescription());
            s.setInt(3, product.getCategoryId());
            s.setString(4, product.getUnit());
            s.setDouble(5, product.getBasePrice());
            s.executeUpdate();
            return generatedId(s);
        }
    }

    public boolean updatePrice(int productId, double newPrice) throws SQLException {
        newPrice = Validation.decimal(newPrice, "Base price", false);
        try (Connection c = connections.open();
                PreparedStatement s =
                        c.prepareStatement("UPDATE products SET base_price=? WHERE id=?")) {
            s.setDouble(1, newPrice);
            s.setInt(2, productId);
            return s.executeUpdate() == 1;
        }
    }

    public boolean updateName(int productId, String newName) throws SQLException {
        newName = Validation.text(newName, "Name", 255);
        try (Connection c = connections.open();
                PreparedStatement s = c.prepareStatement("UPDATE products SET name=? WHERE id=?")) {
            s.setString(1, newName);
            s.setInt(2, productId);
            return s.executeUpdate() == 1;
        }
    }

    public List<ProductCategory> findCategories() throws SQLException {
        try (Connection c = connections.open();
                PreparedStatement s =
                        c.prepareStatement("SELECT * FROM product_categories ORDER BY id");
                ResultSet r = s.executeQuery()) {
            List<ProductCategory> result = new ArrayList<>();
            while (r.next())
                result.add(
                        new ProductCategory(
                                r.getInt("id"), r.getString("name"), r.getString("description")));
            return result;
        }
    }

    public static int generatedId(PreparedStatement s) throws SQLException {
        try (ResultSet r = s.getGeneratedKeys()) {
            if (r.next()) return r.getInt(1);
            throw new SQLException("Generated ID was not returned");
        }
    }
}
