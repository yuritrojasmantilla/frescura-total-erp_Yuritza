package com.frescuratotal.erp.logistics.view;

import com.frescuratotal.erp.logistics.model.*;

public final class LogisticsView {
  public void menu() {
    System.out.println("===========================");
    System.out.println("        LOGISTICS          ");
    System.out.println("===========================");
    System.out.println(
        "1. Orders in preparation\n2. Vehicles"
            + "\n3. Register vehicle\n4. Drivers\n5. Register driver"
            + "\n6. Create route and assign orders\n7. Routes"
            + "\n8. Start route\n9. Mark one order delivered\n0. Back\n");
  }

  public void vehicle(Vehicle v) {
    System.out.println(
        "Vehicle "
            + v.getId()
            + " | "
            + v.getPlate()
            + " | "
            + v.getDescription()
            + " | "
            + (v.isActive() ? "Active" : "Inactive"));
  }

  public void driver(Driver d) {
    System.out.println(
        "Driver "
            + d.getId()
            + " | "
            + d.getName()
            + " | document "
            + d.getDocument()
            + " | license "
            + d.getLicenseNumber()
            + " | "
            + (d.isActive() ? "Active" : "Inactive"));
  }

  public void order(RouteOrder order) {
    System.out.println(
        "Sale "
            + order.getSaleId()
            + " | "
            + order.getSaleNumber()
            + " | "
            + order.getCustomerName()
            + " | "
            + order.getAddress()
            + " | "
            + order.getStatus());
  }

  public void route(DeliveryRoute r) {
    System.out.println(
        "Route "
            + r.getId()
            + " | "
            + r.getRouteNumber()
            + " | "
            + r.getDeliveryDate()
            + " | "
            + r.getStatus()
            + " | vehicle "
            + r.getVehicle().getPlate()
            + " | driver "
            + r.getDriver().getName());
    r.getOrders().forEach(this::order);
    System.out.println();
  }

  public void message(String text) {
    System.out.println(text);
  }
}
