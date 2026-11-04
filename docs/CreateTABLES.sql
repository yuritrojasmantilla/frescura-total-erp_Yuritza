-- *** Script Proyecto ERP Frescura Total **

USE erp_frescura_total;

-- INFORMACIÓN DE LA EMPRESA

CREATE TABLE app_users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_salt CHAR(32) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    role          ENUM('OPERATOR','INSPECTOR') NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);


-- MÓDULO: PROVEEDORES

CREATE TABLE suppliers (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    nit            VARCHAR(20) NOT NULL UNIQUE,
    contact_person VARCHAR(100),
    email          VARCHAR(100) NOT NULL,
    phone          VARCHAR(20),
    address        TEXT,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- MÓDULO: PRODUCTOS

CREATE TABLE product_categories (
	id          INT AUTO_INCREMENT PRIMARY KEY,
	name        VARCHAR(100) NOT NULL UNIQUE,
	description TEXT
);

CREATE TABLE products (
	id          INT AUTO_INCREMENT PRIMARY KEY,
	name        VARCHAR(255) NOT NULL,
	description TEXT,
	category_id INT NOT NULL,
	unit        VARCHAR(20) NOT NULL,
	base_price  DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
	
	FOREIGN KEY (category_id) REFERENCES product_categories(id)
);

CREATE TABLE supplier_products (
    supplier_id   INT NOT NULL,
    product_id    INT NOT NULL,
    PRIMARY KEY (supplier_id,product_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);


-- MÓDULO: INVENTARIO Y CALIDAD

CREATE TABLE lot_statuses (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE inventory_lots (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    lot_number       VARCHAR(50) NOT NULL UNIQUE,
    product_id       INT NOT NULL,
    supplier_id      INT NOT NULL,
    initial_quantity DECIMAL(10,2) NOT NULL CHECK (initial_quantity > 0),
    quantity         DECIMAL(10,2) NOT NULL CHECK (quantity >= 0),
    entry_date       DATE NOT NULL,
    expiration_date  DATE NOT NULL,
    purchase_price   DECIMAL(10,2) NOT NULL CHECK (purchase_price >= 0),
    sale_price       DECIMAL(10,2) NOT NULL CHECK (sale_price >= 0),
    aisle            VARCHAR(50) NOT NULL,
    shelf            VARCHAR(50) NOT NULL,
    status_id        INT NOT NULL,

    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (status_id) REFERENCES lot_statuses(id),
    CHECK (quantity <= initial_quantity),
    CHECK (expiration_date >= entry_date),
    INDEX idx_lot_expiration (expiration_date),
    INDEX idx_lot_product (product_id),
    INDEX idx_lot_supplier (supplier_id)
);

CREATE TABLE quarantine_records (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    lot_id           INT NOT NULL,
    reason           TEXT NOT NULL,
    registered_by    VARCHAR(100) NOT NULL,
    registered_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved         BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at      DATETIME NULL,
    resolution_notes TEXT,
    
    FOREIGN KEY (lot_id) REFERENCES inventory_lots(id),
    FOREIGN KEY (registered_by) REFERENCES app_users(username),
    INDEX idx_quarantine_pending (lot_id,resolved)
);

CREATE TABLE quality_inspections (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    lot_id           INT NOT NULL,
    inspection_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result           ENUM('Approved','Rejected') NOT NULL,
    inspector_name   VARCHAR(100) NOT NULL,
    rejection_reason TEXT,
    notes            TEXT NOT NULL,

    FOREIGN KEY (lot_id) REFERENCES inventory_lots(id),
    FOREIGN KEY (inspector_name) REFERENCES app_users(username)
);

CREATE TABLE waste_records (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    lot_id        INT NOT NULL,
    inspection_id INT NULL,
    quantity_lost DECIMAL(10,2) NOT NULL CHECK (quantity_lost > 0),
    reason        TEXT NOT NULL,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registered_by VARCHAR(100) NOT NULL,

    FOREIGN KEY (lot_id) REFERENCES inventory_lots(id),
    FOREIGN KEY (inspection_id) REFERENCES quality_inspections(id),
    FOREIGN KEY (registered_by) REFERENCES app_users(username)
);


-- MÓDULO: PROMOCIONES

CREATE TABLE promotions (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    status_id           INT NOT NULL UNIQUE,
    discount_percentage DECIMAL(5,2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (status_id) REFERENCES lot_statuses(id)
);

-- MÓDULO: CLIENTES Y VENTAS

CREATE TABLE customer_types (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE sale_statuses (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE customers (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    document      VARCHAR(20) NOT NULL UNIQUE,
    email         VARCHAR(100),
    phone         VARCHAR(20),
    address       TEXT,
    type_id       INT NOT NULL,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (type_id) REFERENCES customer_types(id)
);

CREATE TABLE sales (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sale_number    VARCHAR(50) NOT NULL UNIQUE,
    customer_id    INT NOT NULL,
    sale_date      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal       DECIMAL(12,2) NOT NULL CHECK (subtotal >= 0),
    total_discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_tax      DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount   DECIMAL(12,2) NOT NULL CHECK (total_amount >= 0),
    status_id      INT NOT NULL,
    registered_by  VARCHAR(100),

    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (status_id) REFERENCES sale_statuses(id)
);

CREATE TABLE sale_details (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sale_id         INT NOT NULL,
    lot_id          INT NOT NULL,
    quantity_sold   DECIMAL(10,2) NOT NULL CHECK (quantity_sold > 0),
    unit_price      DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    discount_rate   DECIMAL(5,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_rate        DECIMAL(5,2) NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal        DECIMAL(12,2) NOT NULL,
    line_total      DECIMAL(12,2) NOT NULL,

    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (lot_id) REFERENCES inventory_lots(id)
);

CREATE TABLE invoices (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sale_id        INT NOT NULL UNIQUE,
    simulated_cufe CHAR(68) NOT NULL UNIQUE,
    seller_nit     VARCHAR(20) NOT NULL,
    payment_method ENUM('CASH','CARD','TRANSFER') NOT NULL,
    png_data       LONGBLOB NOT NULL,
    generated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (sale_id) REFERENCES sales(id)
);

CREATE TABLE vehicles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    plate       VARCHAR(12) NOT NULL UNIQUE,
    description VARCHAR(100) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE drivers (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    document       VARCHAR(20) NOT NULL UNIQUE,
    license_number VARCHAR(30) NOT NULL UNIQUE,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE delivery_routes (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    route_number        VARCHAR(50) NOT NULL UNIQUE,
    delivery_date       DATE NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100) NOT NULL,
    estimated_departure DATETIME,
    estimated_arrival   DATETIME,
    actual_departure    DATETIME,
    actual_arrival      DATETIME,
    status              ENUM('Assigned', 'In Transit', 'Delivered', 'Cancelled')
        NOT NULL DEFAULT 'Assigned',

    FOREIGN KEY (created_by) REFERENCES app_users(username)
);

CREATE TABLE delivery_assignments (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    route_id    INT NOT NULL,
    vehicle_id  INT NOT NULL,
    driver_id   INT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100) NOT NULL,
    notes       TEXT,
    
    FOREIGN KEY (route_id) REFERENCES delivery_routes(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    FOREIGN KEY (assigned_by) REFERENCES app_users(username)
);

CREATE TABLE route_sales (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    route_id            INT NOT NULL,
    sale_id             INT NOT NULL UNIQUE,
    delivery_order      INT,
    destination_address TEXT NOT NULL,
    delivered_at        DATETIME,
    received_by         VARCHAR(255),
    notes               TEXT,

    FOREIGN KEY (route_id) REFERENCES delivery_routes(id),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    UNIQUE (route_id, sale_id)
);
