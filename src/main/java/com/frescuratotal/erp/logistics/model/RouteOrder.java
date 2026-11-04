package com.frescuratotal.erp.logistics.model;

public final class RouteOrder {
  private final int saleId;
  private final String saleNumber, customerName, address, status;

  public RouteOrder(
      int saleId, String saleNumber, String customerName, String address, String status) {
    this.saleId = saleId;
    this.saleNumber = saleNumber;
    this.customerName = customerName;
    this.address = address;
    this.status = status;
  }

  public int getSaleId() {
    return saleId;
  }

  public String getSaleNumber() {
    return saleNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getAddress() {
    return address;
  }

  public String getStatus() {
    return status;
  }
}
