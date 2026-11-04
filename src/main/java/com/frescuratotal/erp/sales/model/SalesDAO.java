package com.frescuratotal.erp.sales.model;

import com.frescuratotal.erp.inventory.model.*;

import java.sql.*;
import java.util.*;

public final class SalesDAO {
    public Customer findCustomer(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM customers WHERE id=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Customer not found");
                return mapCustomer(r);
            }
        }
    }

    private Customer mapCustomer(ResultSet r) throws SQLException {
        return new Customer(
                r.getInt("id"),
                r.getString("name"),
                r.getString("document"),
                r.getString("email"),
                r.getString("phone"),
                r.getString("address"),
                r.getInt("type_id"));
    }

    public List<Customer> customers(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM customers ORDER BY id");
                ResultSet r = s.executeQuery()) {
            List<Customer> rows = new ArrayList<>();
            while (r.next()) rows.add(mapCustomer(r));
            return rows;
        }
    }

    public Customer insertCustomer(Connection c, Customer customer) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO customers (name,document,email,phone,address,type_id) VALUES"
                            + " (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, customer.getName());
            s.setString(2, customer.getDocument());
            s.setString(3, customer.getEmail());
            s.setString(4, customer.getPhone());
            s.setString(5, customer.getAddress());
            s.setInt(6, customer.getTypeId());
            s.executeUpdate();
            return findCustomer(c, ProductDAO.generatedId(s));
        }
    }

    public List<Integer> lockLotIds(Connection c, int productId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT id FROM inventory_lots WHERE product_id=? AND quantity>0 ORDER BY"
                            + " expiration_date,id FOR UPDATE")) {
            s.setInt(1, productId);
            try (ResultSet r = s.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (r.next()) ids.add(r.getInt(1));
                return ids;
            }
        }
    }

    public double activeDiscountForStatus(Connection c, int statusId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT discount_percentage FROM promotions WHERE status_id=? AND active=TRUE")) {
            s.setInt(1, statusId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getDouble(1) : 0.0;
            }
        }
    }

    public int insertSale(
            Connection c,
            String number,
            int customerId,
            double subtotal,
            double discount,
            double tax,
            double total,
            String username)
            throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO sales"
                            + " (sale_number,customer_id,subtotal,total_discount,total_tax,total_amount,status_id,registered_by)"
                            + " VALUES (?,?,?,?,?,?,2,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, number);
            s.setInt(2, customerId);
            s.setDouble(3, subtotal);
            s.setDouble(4, discount);
            s.setDouble(5, tax);
            s.setDouble(6, total);
            s.setString(7, username);
            s.executeUpdate();
            return ProductDAO.generatedId(s);
        }
    }

    public void insertLine(Connection c, int saleId, SaleLine line) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO sale_details"
                            + " (sale_id,lot_id,quantity_sold,unit_price,discount_rate,discount_amount,tax_rate,tax_amount,subtotal,line_total)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            s.setInt(1, saleId);
            s.setInt(2, line.getLotId());
            s.setDouble(3, line.getQuantity());
            s.setDouble(4, line.getUnitPrice());
            s.setDouble(5, line.getDiscountRate());
            s.setDouble(6, line.getDiscountAmount());
            s.setDouble(7, line.getTaxRate());
            s.setDouble(8, line.getTaxAmount());
            s.setDouble(9, line.getSubtotal());
            s.setDouble(10, line.getLineTotal());
            s.executeUpdate();
        }
    }

    public void insertInvoice(
            Connection c,
            int saleId,
            String cufe,
            String sellerNit,
            String paymentMethod,
            byte[] png)
            throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement(
                        "INSERT INTO invoices"
                                + " (sale_id,simulated_cufe,seller_nit,payment_method,png_data)"
                                + " VALUES (?,?,?,?,?)")) {
            s.setInt(1, saleId);
            s.setString(2, cufe);
            s.setString(3, sellerNit);
            s.setString(4, paymentMethod);
            s.setBytes(5, png);
            s.executeUpdate();
        }
    }

    public byte[] invoicePng(Connection c, int saleId) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement("SELECT png_data FROM invoices WHERE sale_id=?")) {
            s.setInt(1, saleId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("No invoice for this sale");
                return r.getBytes(1);
            }
        }
    }

    public Sale findSale(Connection c, int id) throws SQLException {
        String sql =
                "SELECT s.*,st.name AS status,c.name AS customer_name,c.document,"
                        + "c.email,c.phone,c.address,c.type_id,i.simulated_cufe,i.payment_method"
                        + " FROM sales s JOIN sale_statuses st ON st.id=s.status_id"
                        + " JOIN customers c ON c.id=s.customer_id"
                        + " LEFT JOIN invoices i ON i.sale_id=s.id WHERE s.id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Sale not found");
                Customer customer =
                        new Customer(
                                r.getInt("customer_id"),
                                r.getString("customer_name"),
                                r.getString("document"),
                                r.getString("email"),
                                r.getString("phone"),
                                r.getString("address"),
                                r.getInt("type_id"));
                return new Sale(
                        id,
                        r.getString("sale_number"),
                        customer,
                        r.getTimestamp("sale_date").toLocalDateTime(),
                        r.getString("status"),
                        r.getString("payment_method"),
                        r.getString("simulated_cufe"),
                        r.getDouble("subtotal"),
                        r.getDouble("total_discount"),
                        r.getDouble("total_tax"),
                        r.getDouble("total_amount"),
                        lines(c, id));
            }
        }
    }

    private List<SaleLine> lines(Connection c, int saleId) throws SQLException {
        String sql =
                "SELECT d.*,l.lot_number,p.id AS product_id,p.name,p.unit"
                        + " FROM sale_details d JOIN inventory_lots l ON l.id=d.lot_id"
                        + " JOIN products p ON p.id=l.product_id WHERE d.sale_id=? ORDER BY d.id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, saleId);
            try (ResultSet r = s.executeQuery()) {
                List<SaleLine> rows = new ArrayList<>();
                while (r.next())
                    rows.add(
                            new SaleLine(
                                    r.getInt("lot_id"),
                                    r.getInt("product_id"),
                                    r.getString("lot_number"),
                                    r.getString("name"),
                                    r.getString("unit"),
                                    r.getDouble("quantity_sold"),
                                    r.getDouble("unit_price"),
                                    r.getDouble("discount_rate"),
                                    r.getDouble("discount_amount"),
                                    r.getDouble("tax_rate"),
                                    r.getDouble("tax_amount"),
                                    r.getDouble("subtotal"),
                                    r.getDouble("line_total")));
                return rows;
            }
        }
    }

    public List<Sale> recent(Connection c, int count) throws SQLException {
        try (PreparedStatement s =
                c.prepareStatement("SELECT id FROM sales ORDER BY id DESC LIMIT ?")) {
            s.setInt(1, count);
            List<Integer> ids = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) ids.add(r.getInt(1));
            }
            List<Sale> rows = new ArrayList<>();
            for (int id : ids) rows.add(findSale(c, id));
            return rows;
        }
    }
}
