package com.frescuratotal.erp.sales.model;

import com.frescuratotal.erp.common.Validation;

public final class Customer {
    private final int id;
    private final String name, document, email, phone, address;
    private final int typeId;

    public Customer(
            int id,
            String name,
            String document,
            String email,
            String phone,
            String address,
            int typeId) {
        if (id < 0 || (typeId != 1 && typeId != 2))
            throw new IllegalArgumentException("Customer ID/type is invalid");
        this.id = id;
        this.name = Validation.text(name, "Customer name", 255);
        this.document = Validation.text(document, "Customer document", 20);
        this.email = email == null ? "" : email.trim();
        this.phone = phone == null ? "" : phone.trim();
        this.address = address == null ? "" : address.trim();
        if (this.email.length() > 100 || this.phone.length() > 20)
            throw new IllegalArgumentException("Customer contact information is too long");
        this.typeId = typeId;
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public int getTypeId() {
        return typeId;
    }
}
