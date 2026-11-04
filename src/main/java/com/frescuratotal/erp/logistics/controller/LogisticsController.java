package com.frescuratotal.erp.logistics.controller;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.logistics.model.*;
import com.frescuratotal.erp.logistics.view.LogisticsView;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public final class LogisticsController {
  private final LogisticsService service;
  private final LogisticsView view;
  private final User user;

  public LogisticsController(LogisticsService service, LogisticsView view, User user) {
    this.service = service;
    this.view = view;
    this.user = user;
  }

  public void runMenu(Scanner scanner) {
    ConsoleInput input = new ConsoleInput(scanner);
    while (true) {
      view.menu();
      try {
        switch (input.integer("Choose an option")) {
          case 1 -> {
            List<RouteOrder> pending = service.pendingOrders();
            if (pending.isEmpty()) view.message("No orders in preparation.");
            pending.forEach(view::order);
          }
          case 2 -> service.vehicles().forEach(view::vehicle);
          case 3 ->
              view.vehicle(
                  service.registerVehicle(
                      user, input.required("Plate"), input.required("Description")));
          case 4 -> service.drivers().forEach(view::driver);
          case 5 ->
              view.driver(
                  service.registerDriver(
                      user,
                      input.required("Name"),
                      input.required("Document"),
                      input.required("License number")));
          case 6 -> assign(input);
          case 7 -> service.routes().forEach(view::route);
          case 8 -> view.route(service.start(user, input.integer("Route ID")));
          case 9 ->
              view.route(
                  service.deliver(
                      user, input.integer("Route ID"), input.integer("Sale ID delivered")));
          case 0 -> {
            return;
          }
          default -> view.message("Invalid option.");
        }
      } catch (SQLException | IllegalArgumentException | IllegalStateException error) {
        view.message("Error: " + error.getMessage());
      }
    }
  }

  private void assign(ConsoleInput input) throws SQLException {
    List<RouteOrder> pending = service.pendingOrders();
    if (pending.isEmpty()) {
      view.message("No orders in preparation.");
      return;
    }
    pending.forEach(view::order);
    service.vehicles().forEach(view::vehicle);
    int vehicle = input.integer("Vehicle ID");
    service.drivers().forEach(view::driver);
    int driver = input.integer("Driver ID");
    LocalDate date = input.date("Delivery date");
    List<Integer> ids = new ArrayList<>();
    while (true) {
      int id = input.integer("Sale ID for this route (0 to finish)");
      if (id == 0) break;
      ids.add(id);
    }
    view.route(service.assign(user, vehicle, driver, date, ids));
  }
}
