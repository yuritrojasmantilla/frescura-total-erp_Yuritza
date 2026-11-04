package com.frescuratotal.erp.logistics.model;

import com.frescuratotal.erp.common.Validation;

public final class Driver {
  private final int id;
  private final String name;
  private final String document;
  private final String licenseNumber;
  private final boolean active;

  public Driver(int id, String name, String document, String licenseNumber, boolean active) {
    if (id < 0) throw new IllegalArgumentException("Driver ID cannot be negative");
    this.id = id;
    this.name = Validation.text(name, "Driver name", 150);
    this.document = Validation.text(document, "Driver document", 20);
    this.licenseNumber = Validation.text(licenseNumber, "License number", 30);
    this.active = active;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDocument() {
    return document;
  }

  public String getLicenseNumber() {
    return licenseNumber;
  }

  public boolean isActive() {
    return active;
  }
}
