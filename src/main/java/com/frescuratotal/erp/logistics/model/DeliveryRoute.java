package com.frescuratotal.erp.logistics.model;

import java.time.LocalDate;
import java.util.List;

public final class DeliveryRoute {
  private final int id;
  private final String routeNumber, status;
  private final Vehicle vehicle;
  private final Driver driver;
  private final LocalDate deliveryDate;
  private final List<RouteOrder> orders;

  public DeliveryRoute(
      int id,
      String routeNumber,
      Vehicle vehicle,
      Driver driver,
      LocalDate deliveryDate,
      String status,
      List<RouteOrder> orders) {
    this.id = id;
    this.routeNumber = routeNumber;
    this.vehicle = vehicle;
    this.driver = driver;
    this.deliveryDate = deliveryDate;
    this.status = status;
    this.orders = List.copyOf(orders);
  }

  public int getId() {
    return id;
  }

  public String getRouteNumber() {
    return routeNumber;
  }

  public Vehicle getVehicle() {
    return vehicle;
  }

  public Driver getDriver() {
    return driver;
  }

  public LocalDate getDeliveryDate() {
    return deliveryDate;
  }

  public String getStatus() {
    return status;
  }

  public List<RouteOrder> getOrders() {
    return orders;
  }
}
