package com.frescuratotal.erp.sales.model;

import com.frescuratotal.erp.common.*;
import com.frescuratotal.erp.inventory.model.*;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class SalesService {
    private final ConnectionProvider connections;
    private final Clock clock;
    private final String sellerNit;
    private final SalesDAO dao = new SalesDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final InventoryService inventory;
    private final InvoiceRenderer renderer = new InvoiceRenderer();

    public SalesService(InventoryService inventory) {
        this(
                DatabaseConnection::getConnection,
                Clock.systemDefaultZone(),
                System.getenv("COMPANY_NIT"),
                inventory);
    }

    public SalesService(
            ConnectionProvider connections,
            Clock clock,
            String sellerNit,
            InventoryService inventory) {
        this.connections = connections;
        this.clock = clock;
        this.sellerNit = sellerNit;
        this.inventory = inventory;
    }

    public List<Customer> customers() throws SQLException {
        try (Connection c = connections.open()) {
            return dao.customers(c);
        }
    }

    public Customer registerCustomer(User user, Customer customer) throws SQLException {
        try (Connection c = connections.open()) {
            requireUser(c, user);
            return dao.insertCustomer(c, customer);
        }
    }

    public List<Lot> sellable(int productId) throws SQLException {
        return inventory.findSellableLots(productId);
    }

    public Sale findSale(int id) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.findSale(c, id);
        }
    }

    public List<Sale> recent(int count) throws SQLException {
        try (Connection c = connections.open()) {
            return dao.recent(c, count);
        }
    }

    private void requireUser(Connection c, User user) throws SQLException {
        if (user == null) throw new IllegalStateException("Login required");
        try (PreparedStatement s =
                c.prepareStatement(
                        "SELECT 1 FROM app_users WHERE" + " id=? AND username=? AND active=TRUE")) {
            s.setInt(1, user.getId());
            s.setString(2, user.getUsername());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("User is inactive");
            }
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double bounded(double value, String label) {
        if (!Double.isFinite(value) || value < 0 || value > 99999999.99)
            throw new IllegalArgumentException(label + " exceeds DECIMAL(10,2) capacity");
        return round(value);
    }

    private String simulatedCufe(Sale sale) {
        String data =
                sale.getNumber()
                        + "|"
                        + sale.getCustomer().getDocument()
                        + "|"
                        + sale.getDate()
                        + "|"
                        + sale.getTotal()
                        + "|"
                        + UUID.randomUUID();
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "SIM-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    public Sale confirm(
            User user, int customerId, List<SaleItemRequest> requests, String paymentMethod)
            throws SQLException, IOException {
        String nit = Validation.text(sellerNit, "COMPANY_NIT", 20);
        String payment =
                Validation.text(paymentMethod, "Payment method", 20).toUpperCase(Locale.ROOT);
        if (!List.of("CASH", "CARD", "TRANSFER").contains(payment))
            throw new IllegalArgumentException("Payment method: CASH, CARD or TRANSFER");
        if (requests == null || requests.isEmpty())
            throw new IllegalArgumentException("Add at least one product");
        if (requests.size() > 100) throw new IllegalArgumentException("Too many products");
        Set<Integer> unique = new HashSet<>();
        for (SaleItemRequest request : requests) {
            if (request == null || !unique.add(request.getProductId()))
                throw new IllegalArgumentException("Product IDs cannot be duplicated");
        }
        inventory.refreshIfNewDay();
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                requireUser(c, user);
                Customer customer = dao.findCustomer(c, customerId);
                LocalDate date = LocalDate.now(clock);
                double nearExpiryDiscount = dao.activeDiscountForStatus(c, Lot.NEAR_EXPIRY);
                List<SaleLine> lines = new ArrayList<>();
                List<Lot> chosen = new ArrayList<>();
                // Stable product order avoids deadlocks when two orders overlap.
                for (SaleItemRequest request :
                        requests.stream()
                                .sorted(Comparator.comparingInt(SaleItemRequest::getProductId))
                                .toList()) {
                    double remaining = request.getQuantity();
                    for (int lotId : dao.lockLotIds(c, request.getProductId())) {
                        // A locking read sees the latest committed stock, even if an earlier
                        // customer lookup established a repeatable-read snapshot.
                        Lot lot = inventoryDAO.findById(c, lotId, true);
                        lot.updateStatus(date);
                        if (!lot.isSellable(date)) continue;
                        double take = round(Math.min(remaining, lot.getQuantity()));
                        if (take <= 0) continue;
                        double gross = bounded(lot.getSalePrice() * take, "Line subtotal");
                        double rate = lot.getStatusId() == Lot.NEAR_EXPIRY ? nearExpiryDiscount : 0.0;
                        double discount = bounded(gross * rate / 100.0, "Line discount");
                        double tax =
                                bounded(
                                        (gross - discount) * request.getTaxRate() / 100.0,
                                        "Line tax");
                        double total = bounded(gross - discount + tax, "Line total");
                        lines.add(
                                new SaleLine(
                                        lotId,
                                        request.getProductId(),
                                        lot.getLotNumber(),
                                        lot.getProduct().getName(),
                                        lot.getProduct().getUnit(),
                                        take,
                                        lot.getSalePrice(),
                                        rate,
                                        discount,
                                        request.getTaxRate(),
                                        tax,
                                        gross,
                                        total));
                        lot.removeForSale(take, date);
                        chosen.add(lot);
                        remaining = round(remaining - take);
                        if (remaining == 0) break;
                    }
                    if (remaining > 0)
                        throw new IllegalStateException(
                                "Insufficient sellable stock for product "
                                        + request.getProductId()
                                        + "; missing "
                                        + remaining);
                }
                double subtotal = round(lines.stream().mapToDouble(SaleLine::getSubtotal).sum());
                double discount =
                        round(lines.stream().mapToDouble(SaleLine::getDiscountAmount).sum());
                double tax = round(lines.stream().mapToDouble(SaleLine::getTaxAmount).sum());
                double total = round(subtotal - discount + tax);
                if (subtotal > 9999999999.99
                        || discount > 9999999999.99
                        || tax > 9999999999.99
                        || total > 9999999999.99)
                    throw new IllegalArgumentException("Sale total exceeds database capacity");
                String number = "FT-" + UUID.randomUUID();
                int saleId =
                        dao.insertSale(
                                c,
                                number,
                                customerId,
                                subtotal,
                                discount,
                                tax,
                                total,
                                user.getUsername());
                for (SaleLine line : lines) dao.insertLine(c, saleId, line);
                for (Lot lot : chosen) inventoryDAO.saveStock(c, lot);
                Sale provisional = dao.findSale(c, saleId);
                String cufe = simulatedCufe(provisional);
                Sale invoice =
                        new Sale(
                                saleId,
                                number,
                                customer,
                                provisional.getDate(),
                                "In Preparation",
                                payment,
                                cufe,
                                subtotal,
                                discount,
                                tax,
                                total,
                                lines);
                byte[] png = renderer.render(invoice, nit);
                dao.insertInvoice(c, saleId, cufe, nit, payment, png);
                c.commit();
                AppLogger.info("Sale confirmed: ID " + saleId + ", " + lines.size() + " lot lines");
                return invoice;
            } catch (SQLException | IOException | RuntimeException error) {
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    error.addSuppressed(rollback);
                }
                throw error;
            }
        }
    }

    public Path exportInvoice(int saleId, Path directory) throws SQLException, IOException {
        if (saleId <= 0) throw new IllegalArgumentException("Sale ID must be positive");
        byte[] png;
        try (Connection c = connections.open()) {
            png = dao.invoicePng(c, saleId);
        }
        Path folder = directory.toAbsolutePath().normalize();
        Files.createDirectories(folder);
        Path target = folder.resolve("invoice-" + saleId + ".png");
        Path temp = Files.createTempFile(folder, "invoice-", ".tmp");
        try {
            Files.write(temp, png);
            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
        AppLogger.info("Invoice exported for sale ID " + saleId);
        return target;
    }
}
