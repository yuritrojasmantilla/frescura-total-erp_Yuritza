package com.frescuratotal.erp.suppliers.model;

import com.frescuratotal.erp.common.Validation;

public final class SupplierProfile {
    private final int id;
    private final String name;
    private final String nit;
    private final String contactPerson;
    private final String email;
    private final String phone;
    private final String address;
    private final boolean active;

    public SupplierProfile(
            int id,
            String name,
            String nit,
            String contactPerson,
            String email,
            String phone,
            String address,
            boolean active) {
        if (id < 0) throw new IllegalArgumentException("Supplier ID cannot be negative");
        this.id = id;
        this.name = Validation.text(name, "Supplier name", 255);
        this.nit = Validation.text(nit, "NIT", 20);
        this.contactPerson = contactPerson == null ? "" : contactPerson.trim();
        this.email = Validation.text(email, "Email", 100);
        this.phone = phone == null ? "" : phone.trim();
        this.address = address == null ? "" : address.trim();
        if (this.contactPerson.length() > 100 || this.phone.length() > 20)
            throw new IllegalArgumentException("Supplier contact information is too long");
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNit() {
        return nit;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
