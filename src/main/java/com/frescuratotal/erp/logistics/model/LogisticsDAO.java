package com.frescuratotal.erp.logistics.model;

import com.frescuratotal.erp.inventory.model.ProductDAO;
import java.sql.*;
import java.util.*;

public final class LogisticsDAO {
  private Vehicle vehicle(ResultSet r) throws SQLException {
    return new Vehicle(
        r.getInt("id"), r.getString("plate"), r.getString("description"), r.getBoolean("active"));
  }

  private Driver driver(ResultSet r) throws SQLException {
    return new Driver(
        r.getInt("id"),
        r.getString("name"),
        r.getString("document"),
        r.getString("license_number"),
        r.getBoolean("active"));
  }

  public List<Vehicle> vehicles(Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement("SELECT * FROM vehicles ORDER BY id");
        ResultSet r = s.executeQuery()) {
      List<Vehicle> rows = new ArrayList<>();
      while (r.next()) rows.add(vehicle(r));
      return rows;
    }
  }

  public List<Driver> drivers(Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement("SELECT * FROM drivers ORDER BY id");
        ResultSet r = s.executeQuery()) {
      List<Driver> rows = new ArrayList<>();
      while (r.next()) rows.add(driver(r));
      return rows;
    }
  }

  public Vehicle findVehicle(Connection c, int id, boolean lock) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("SELECT * FROM vehicles WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
      s.setInt(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new IllegalArgumentException("Vehicle not found");
        return vehicle(r);
      }
    }
  }

  public Driver findDriver(Connection c, int id, boolean lock) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("SELECT * FROM drivers WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
      s.setInt(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new IllegalArgumentException("Driver not found");
        return driver(r);
      }
    }
  }

  public int insertVehicle(Connection c, Vehicle v) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "INSERT INTO vehicles" + " (plate,description) VALUES (?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
      s.setString(1, v.getPlate());
      s.setString(2, v.getDescription());
      s.executeUpdate();
      return ProductDAO.generatedId(s);
    }
  }

  public int insertDriver(Connection c, Driver d) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "INSERT INTO drivers" + " (name,document,license_number) VALUES (?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
      s.setString(1, d.getName());
      s.setString(2, d.getDocument());
      s.setString(3, d.getLicenseNumber());
      s.executeUpdate();
      return ProductDAO.generatedId(s);
    }
  }

  public List<RouteOrder> pending(Connection c) throws SQLException {
    String sql =
        "SELECT s.id,s.sale_number,c.name,c.address,st.name AS status"
            + " FROM sales s JOIN customers c ON c.id=s.customer_id"
            + " JOIN sale_statuses st ON st.id=s.status_id WHERE s.status_id=2"
            + " AND NOT EXISTS (SELECT 1 FROM route_sales rs WHERE rs.sale_id=s.id)"
            + " ORDER BY s.id";
    try (PreparedStatement s = c.prepareStatement(sql);
        ResultSet r = s.executeQuery()) {
      List<RouteOrder> rows = new ArrayList<>();
      while (r.next()) rows.add(mapOrder(r));
      return rows;
    }
  }

  private RouteOrder mapOrder(ResultSet r) throws SQLException {
    return new RouteOrder(
        r.getInt("id"),
        r.getString("sale_number"),
        r.getString("name"),
        r.getString("address"),
        r.getString("status"));
  }

  public int insertRoute(
      Connection c,
      String number,
      java.time.LocalDate date,
      String username)
      throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "INSERT INTO delivery_routes"
                + " (route_number,delivery_date,status,created_by)"
                + " VALUES (?,?,'Assigned',?)",
            Statement.RETURN_GENERATED_KEYS)) {
      s.setString(1, number);
      s.setDate(2, java.sql.Date.valueOf(date));
      s.setString(3, username);
      s.executeUpdate();
      return ProductDAO.generatedId(s);
    }
  }

  public void assignVehicleAndDriver(Connection c, int routeId, int vehicleId,
      int driverId, String username) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
        "INSERT INTO delivery_assignments"
            + " (route_id,vehicle_id,driver_id,assigned_by) VALUES (?,?,?,?)")) {
      s.setInt(1, routeId);
      s.setInt(2, vehicleId);
      s.setInt(3, driverId);
      s.setString(4, username);
      s.executeUpdate();
    }
  }

  public void assign(Connection c, int routeId, int saleId, int deliveryOrder) throws SQLException {
    String address;
    try (PreparedStatement sale =
        c.prepareStatement("SELECT s.status_id,c.address FROM sales s"
            + " JOIN customers c ON c.id=s.customer_id WHERE s.id=? FOR UPDATE")) {
      sale.setInt(1, saleId);
      try (ResultSet r = sale.executeQuery()) {
        if (!r.next() || r.getInt(1) != 2)
          throw new IllegalStateException("Sale is not in preparation");
        address = r.getString(2);
        if (address == null || address.isBlank())
          throw new IllegalStateException("Customer delivery address is required");
      }
    }
    try (PreparedStatement s =
        c.prepareStatement("INSERT INTO route_sales"
            + " (route_id,sale_id,delivery_order,destination_address) VALUES (?,?,?,?)")) {
      s.setInt(1, routeId);
      s.setInt(2, saleId);
      s.setInt(3, deliveryOrder);
      s.setString(4, address);
      s.executeUpdate();
    }
  }

  public String lockRouteStatus(Connection c, int routeId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("SELECT status FROM delivery_routes" + " WHERE id=? FOR UPDATE")) {
      s.setInt(1, routeId);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new IllegalArgumentException("Route not found");
        return r.getString(1);
      }
    }
  }

  public int startOrders(Connection c, int routeId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "UPDATE sales s JOIN route_sales rs"
                + " ON rs.sale_id=s.id SET s.status_id=3"
                + " WHERE rs.route_id=? AND s.status_id=2")) {
      s.setInt(1, routeId);
      return s.executeUpdate();
    }
  }

  public int orderCount(Connection c, int routeId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("SELECT COUNT(*) FROM route_sales" + " WHERE route_id=?")) {
      s.setInt(1, routeId);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return r.getInt(1);
      }
    }
  }

  public void deliverOrder(Connection c, int routeId, int saleId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "UPDATE sales s JOIN route_sales rs"
                + " ON rs.sale_id=s.id SET s.status_id=4"
                + " WHERE rs.route_id=? AND s.id=? AND s.status_id=3")) {
      s.setInt(1, routeId);
      s.setInt(2, saleId);
      if (s.executeUpdate() != 1)
        throw new IllegalStateException("This order is not in transit on the selected route");
    }
    try (PreparedStatement s = c.prepareStatement(
        "UPDATE route_sales SET delivered_at=CURRENT_TIMESTAMP"
            + " WHERE route_id=? AND sale_id=?")) {
      s.setInt(1, routeId);
      s.setInt(2, saleId);
      s.executeUpdate();
    }
  }

  public int undeliveredCount(Connection c, int routeId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "SELECT COUNT(*) FROM route_sales rs"
                + " JOIN sales sale ON sale.id=rs.sale_id WHERE rs.route_id=?"
                + " AND sale.status_id<>4")) {
      s.setInt(1, routeId);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return r.getInt(1);
      }
    }
  }

  public void status(Connection c, int routeId, String status) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("UPDATE delivery_routes SET status=?,"
            + " actual_departure=CASE WHEN ?='In Transit' THEN CURRENT_TIMESTAMP"
            + " ELSE actual_departure END,"
            + " actual_arrival=CASE WHEN ?='Delivered' THEN CURRENT_TIMESTAMP"
            + " ELSE actual_arrival END WHERE id=?")) {
      s.setString(1, status);
      s.setString(2, status);
      s.setString(3, status);
      s.setInt(4, routeId);
      if (s.executeUpdate() != 1) throw new SQLException("Route status could not be saved");
    }
  }

  public DeliveryRoute findRoute(Connection c, int routeId) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
        "SELECT r.id,r.route_number,r.delivery_date,r.status,a.vehicle_id,a.driver_id"
            + " FROM delivery_routes r JOIN delivery_assignments a ON a.route_id=r.id"
            + " WHERE r.id=? ORDER BY a.id LIMIT 1")) {
      s.setInt(1, routeId);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new IllegalArgumentException("Route not found");
        return new DeliveryRoute(
            routeId,
            r.getString("route_number"),
            findVehicle(c, r.getInt("vehicle_id"), false),
            findDriver(c, r.getInt("driver_id"), false),
            r.getDate("delivery_date").toLocalDate(),
            r.getString("status"),
            routeOrders(c, routeId));
      }
    }
  }

  public List<RouteOrder> routeOrders(Connection c, int routeId) throws SQLException {
    String sql =
        "SELECT s.id,s.sale_number,c.name,c.address,st.name AS status"
            + " FROM route_sales rs JOIN sales s ON s.id=rs.sale_id"
            + " JOIN customers c ON c.id=s.customer_id"
            + " JOIN sale_statuses st ON st.id=s.status_id"
            + " WHERE rs.route_id=? ORDER BY rs.delivery_order,s.id";
    try (PreparedStatement s = c.prepareStatement(sql)) {
      s.setInt(1, routeId);
      try (ResultSet r = s.executeQuery()) {
        List<RouteOrder> rows = new ArrayList<>();
        while (r.next()) rows.add(mapOrder(r));
        return rows;
      }
    }
  }

  public List<DeliveryRoute> routes(Connection c) throws SQLException {
    List<Integer> ids = new ArrayList<>();
    try (PreparedStatement s =
            c.prepareStatement("SELECT id FROM delivery_routes ORDER BY id DESC");
        ResultSet r = s.executeQuery()) {
      while (r.next()) ids.add(r.getInt(1));
    }
    List<DeliveryRoute> result = new ArrayList<>();
    for (int id : ids) result.add(findRoute(c, id));
    return result;
  }
}
