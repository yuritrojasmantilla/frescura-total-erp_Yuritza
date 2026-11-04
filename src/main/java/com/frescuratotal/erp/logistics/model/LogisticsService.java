package com.frescuratotal.erp.logistics.model;

import com.frescuratotal.erp.common.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class LogisticsService {
  private final ConnectionProvider connections;
  private final LogisticsDAO dao = new LogisticsDAO();

  public LogisticsService() {
    this(DatabaseConnection::getConnection);
  }

  public LogisticsService(ConnectionProvider connections) {
    this.connections = connections;
  }

  private void checkUser(Connection c, User user) throws SQLException {
    if (user == null) throw new IllegalStateException("Login required");
    try (PreparedStatement s =
        c.prepareStatement(
            "SELECT 1 FROM app_users" + " WHERE id=? AND username=? AND active=TRUE")) {
      s.setInt(1, user.getId());
      s.setString(2, user.getUsername());
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new IllegalStateException("User is inactive");
      }
    }
  }

  public List<Vehicle> vehicles() throws SQLException {
    try (Connection c = connections.open()) {
      return dao.vehicles(c);
    }
  }

  public List<Driver> drivers() throws SQLException {
    try (Connection c = connections.open()) {
      return dao.drivers(c);
    }
  }

  public Vehicle registerVehicle(User user, String plate, String description) throws SQLException {
    Vehicle newVehicle = new Vehicle(0, plate, description, true);
    try (Connection c = connections.open()) {
      checkUser(c, user);
      return dao.findVehicle(c, dao.insertVehicle(c, newVehicle), false);
    }
  }

  public Driver registerDriver(User user, String name, String document, String license)
      throws SQLException {
    Driver newDriver = new Driver(0, name, document, license, true);
    try (Connection c = connections.open()) {
      checkUser(c, user);
      return dao.findDriver(c, dao.insertDriver(c, newDriver), false);
    }
  }

  public List<RouteOrder> pendingOrders() throws SQLException {
    try (Connection c = connections.open()) {
      return dao.pending(c);
    }
  }

  public List<DeliveryRoute> routes() throws SQLException {
    try (Connection c = connections.open()) {
      return dao.routes(c);
    }
  }

  public DeliveryRoute route(int id) throws SQLException {
    try (Connection c = connections.open()) {
      return dao.findRoute(c, id);
    }
  }

  public DeliveryRoute assign(
      User user, int vehicleId, int driverId, LocalDate deliveryDate, List<Integer> saleIds)
      throws SQLException {
    if (deliveryDate == null || deliveryDate.isBefore(LocalDate.now()))
      throw new IllegalArgumentException("Delivery date cannot be in the past");
    if (saleIds == null
        || saleIds.isEmpty()
        || saleIds.size() > 100
        || saleIds.stream().anyMatch(id -> id == null || id <= 0)
        || new HashSet<>(saleIds).size() != saleIds.size())
      throw new IllegalArgumentException("Select 1 to 100 different sale IDs");
    try (Connection c = connections.open()) {
      c.setAutoCommit(false);
      try {
        checkUser(c, user);
        if (!dao.findVehicle(c, vehicleId, true).isActive()
            || !dao.findDriver(c, driverId, true).isActive())
          throw new IllegalStateException("Vehicle and driver must be active");
        String number = "ROUTE-" + UUID.randomUUID();
        int id = dao.insertRoute(c, number, deliveryDate, user.getUsername());
        dao.assignVehicleAndDriver(c, id, vehicleId, driverId, user.getUsername());
        // Preserve the delivery order entered in the menu.
        int deliveryOrder = 1;
        for (int saleId : saleIds) dao.assign(c, id, saleId, deliveryOrder++);
        DeliveryRoute route = dao.findRoute(c, id);
        c.commit();
        AppLogger.info("Route assigned: ID " + id + ", " + saleIds.size() + " sales");
        return route;
      } catch (SQLException | RuntimeException error) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          error.addSuppressed(rollback);
        }
        throw error;
      }
    }
  }

  public DeliveryRoute start(User user, int routeId) throws SQLException {
    try (Connection c = connections.open()) {
      c.setAutoCommit(false);
      try {
        checkUser(c, user);
        if (!"Assigned".equals(dao.lockRouteStatus(c, routeId)))
          throw new IllegalStateException("Only assigned routes can start");
        int count = dao.orderCount(c, routeId);
        if (count == 0 || dao.startOrders(c, routeId) != count)
          throw new IllegalStateException("All orders must be in preparation");
        dao.status(c, routeId, "In Transit");
        DeliveryRoute route = dao.findRoute(c, routeId);
        c.commit();
        AppLogger.info("Route started: ID " + routeId);
        return route;
      } catch (SQLException | RuntimeException error) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          error.addSuppressed(rollback);
        }
        throw error;
      }
    }
  }

  public DeliveryRoute deliver(User user, int routeId, int saleId) throws SQLException {
    try (Connection c = connections.open()) {
      c.setAutoCommit(false);
      try {
        checkUser(c, user);
        if (!"In Transit".equals(dao.lockRouteStatus(c, routeId)))
          throw new IllegalStateException("Route is not in transit");
        dao.deliverOrder(c, routeId, saleId);
        if (dao.undeliveredCount(c, routeId) == 0) dao.status(c, routeId, "Delivered");
        DeliveryRoute route = dao.findRoute(c, routeId);
        c.commit();
        AppLogger.info("Sale " + saleId + " delivered on route " + routeId);
        return route;
      } catch (SQLException | RuntimeException error) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          error.addSuppressed(rollback);
        }
        throw error;
      }
    }
  }
}
