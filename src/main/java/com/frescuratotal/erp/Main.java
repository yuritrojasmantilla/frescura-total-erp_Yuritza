package com.frescuratotal.erp;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.inventory.controller.*;
import com.frescuratotal.erp.inventory.model.*;
import com.frescuratotal.erp.inventory.view.*;
import com.frescuratotal.erp.logistics.controller.LogisticsController;
import com.frescuratotal.erp.logistics.model.LogisticsService;
import com.frescuratotal.erp.logistics.view.LogisticsView;
import com.frescuratotal.erp.sales.controller.SalesController;
import com.frescuratotal.erp.sales.model.SalesService;
import com.frescuratotal.erp.sales.view.SalesView;
import com.frescuratotal.erp.suppliers.controller.SupplierController;
import com.frescuratotal.erp.suppliers.model.SupplierService;
import com.frescuratotal.erp.suppliers.view.SupplierView;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class Main {
  public static void main(String[] args) {
    System.out.println("Welcome to Frescura Total ERP - Modules 1 to 4");
    InventoryService service = new InventoryService();
    ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "daily-inventory-check");
              t.setDaemon(true);
              return t;
            });
    try (Scanner scanner = new Scanner(System.in)) {
      ConsoleInput input = new ConsoleInput(scanner);
      AuthDAO auth = new AuthDAO();
      User user = null;
      while (user == null) {
        String username = input.text("Username (0 to exit)");
        if (username.equals("0")) return;
        try {
          user = auth.login(username, input.text("Password"));
        } catch (IllegalArgumentException error) {
          System.out.println(error.getMessage());
        }
      }
      service.refreshIfNewDay();
      scheduler.scheduleWithFixedDelay(
          () -> {
            try {
              service.refreshIfNewDay();
            } catch (SQLException error) {
              AppLogger.error("Automatic expiration check failed", error);
              System.err.println("Automatic expiration check failed: " + error.getMessage());
            }
          },
          60,
          60,
          TimeUnit.SECONDS);
      System.out.println("Logged in as " + user.getUsername() + " | Role: " + user.getRole());
      AppLogger.info("Login successful: " + user.getUsername());
      ProductController products = new ProductController(new ProductView(), new ProductDAO());
      LotController lots = new LotController(new LotView(), service, user);
      SupplierController suppliers =
          new SupplierController(new SupplierService(service), new SupplierView(), user);
      SalesController sales = new SalesController(new SalesService(service), new SalesView(), user);
      LogisticsController logistics =
          new LogisticsController(new LogisticsService(), new LogisticsView(), user);
      long nearCount =
          service.findLots(null, null, null).stream()
              .filter(l -> l.getQuantity() > 0 && l.getStatusId() == Lot.NEAR_EXPIRY)
              .count();
      long expiredCount =
          service.findLots(null, null, null).stream()
              .filter(l -> l.getQuantity() > 0 && l.isExpired(service.today()))
              .count();
      System.out.println(
          "Expiration alerts: "
              + nearCount
              + " near-expiry lots; "
              + expiredCount
              + " expired lots with stock. See Inventory option 5.");
      while (true) {
        System.out.println();
        System.out.println("====================");
        System.out.println("    MAIN MENU       ");
        System.out.println("====================");
        System.out.println(
            "1. Products\n2. Inventory and quality"
                + "\n3. Suppliers and purchases\n4. Sales and billing"
                + "\n5. Logistics and deliveries\n0. Exit\n");
        switch (input.integer("Choose an option")) {
          case 1 -> products.runMenu(scanner);
          case 2 -> lots.runMenu(scanner);
          case 3 -> suppliers.runMenu(scanner);
          case 4 -> sales.runMenu(scanner);
          case 5 -> logistics.runMenu(scanner);
          case 0 -> {
            return;
          }
          default -> System.out.println("Invalid option.");
        }
      }
    } catch (SQLException error) {
      AppLogger.error("Database operation failed", error);
      System.err.println("Database error: " + error.getMessage());
      System.err.println(
          "Verify DB_PASSWORD, the selected database, and the module SQL installation.");
    } catch (NoSuchElementException error) {
      System.out.println("Input closed. Goodbye.");
    } finally {
      scheduler.shutdownNow();
    }
  }
}
