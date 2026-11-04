package com.frescuratotal.erp.inventory.model;

public final class Supplier {
    private final int id;
    private final String name;
    private final String nit;
    private final String email;

    public Supplier(int id, String name, String nit, String email) {
        this.id = id;
        this.name = name;
        this.nit = nit;
        this.email = email;
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

    public String getEmail() {
        return email;
    }
}
