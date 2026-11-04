package com.frescuratotal.erp.sales.model;

import java.time.LocalDateTime;
import java.util.List;

public final class Sale {
    private final int id;
    private final String number, status, paymentMethod, simulatedCufe;
    private final Customer customer;
    private final LocalDateTime date;
    private final double subtotal, discount, tax, total;
    private final List<SaleLine> lines;

    public Sale(
            int id,
            String number,
            Customer customer,
            LocalDateTime date,
            String status,
            String paymentMethod,
            String simulatedCufe,
            double subtotal,
            double discount,
            double tax,
            double total,
            List<SaleLine> lines) {
        this.id = id;
        this.number = number;
        this.customer = customer;
        this.date = date;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.simulatedCufe = simulatedCufe;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.lines = List.copyOf(lines);
    }

    public int getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getSimulatedCufe() {
        return simulatedCufe;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTax() {
        return tax;
    }

    public double getTotal() {
        return total;
    }

    public List<SaleLine> getLines() {
        return lines;
    }
}
