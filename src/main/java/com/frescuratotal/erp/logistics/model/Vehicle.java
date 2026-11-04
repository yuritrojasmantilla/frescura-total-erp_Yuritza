package com.frescuratotal.erp.logistics.model;

import com.frescuratotal.erp.common.Validation;

public final class Vehicle {
  private final int id;
  private final String plate;
  private final String description;
  private final boolean active;

  public Vehicle(int id, String plate, String description, boolean active) {
    if (id < 0) throw new IllegalArgumentException("Vehicle ID cannot be negative");
    this.id = id;
    this.plate = Validation.text(plate, "Vehicle plate", 12).toUpperCase();
    this.description = Validation.text(description, "Vehicle description", 100);
    this.active = active;
  }

  public int getId() {
    return id;
  }

  public String getPlate() {
    return plate;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }
}
