package com.frescuratotal.erp.sales.model;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

/** Produces an academic image, never a DIAN-validated electronic invoice. */
public final class InvoiceRenderer {
    private static final int WIDTH = 1500;

    public byte[] render(Sale sale, String sellerNit) throws IOException {
        int height = 780 + sale.getLines().size() * 40;
        BufferedImage canvas = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, height);
            g.setColor(new Color(22, 94, 62));
            g.fillRect(0, 0, WIDTH, 125);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 34));
            g.drawString("FRESCURA TOTAL", 65, 52);
            g.setFont(new Font("SansSerif", Font.PLAIN, 21));
            g.drawString("COMPROBANTE VISUAL DE VENTA", 65, 86);
            g.setColor(new Color(155, 31, 31));
            g.setFont(new Font("SansSerif", Font.BOLD, 23));
            g.drawString("SIMULACION ACADEMICA - NO VALIDA ANTE LA DIAN", 65, 163);
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 19));
            g.drawString("Vendedor: Frescura Total   NIT: " + sellerNit, 65, 205);
            g.drawString(
                    "Numero interno: "
                            + sale.getNumber()
                            + "   Fecha: "
                            + sale.getDate().toString().replace('T', ' '),
                    65,
                    238);
            g.drawString(
                    "Cliente: "
                            + sale.getCustomer().getName()
                            + "   Documento: "
                            + sale.getCustomer().getDocument(),
                    65,
                    271);
            g.drawString(
                    "Tipo: "
                            + (sale.getCustomer().getTypeId() == 1 ? "Wholesale" : "Retail")
                            + "   Pago: contado / "
                            + sale.getPaymentMethod(),
                    65,
                    304);
            g.drawString("Direccion: " + shorten(sale.getCustomer().getAddress(), 80), 65, 337);
            g.setColor(new Color(222, 235, 226));
            g.fillRect(55, 360, 1390, 42);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Producto / lote", 65, 388);
            g.drawString("Cantidad", 510, 388);
            g.drawString("Precio", 660, 388);
            g.drawString("Descuento", 815, 388);
            g.drawString("Impuesto", 1010, 388);
            g.drawString("Total", 1270, 388);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int y = 428;
            for (SaleLine line : sale.getLines()) {
                g.drawString(
                        shorten(line.getProductName(), 26)
                                + " / "
                                + shorten(line.getLotNumber(), 17),
                        65,
                        y);
                g.drawString(money(line.getQuantity()) + " " + line.getUnit(), 510, y);
                g.drawString(money(line.getUnitPrice()), 660, y);
                g.drawString(
                        money(line.getDiscountAmount())
                                + " ("
                                + money(line.getDiscountRate())
                                + "%)",
                        815,
                        y);
                g.drawString(
                        money(line.getTaxAmount()) + " (" + money(line.getTaxRate()) + "%)",
                        1010,
                        y);
                g.drawString(money(line.getLineTotal()), 1270, y);
                y += 40;
            }
            y += 16;
            g.setColor(new Color(22, 94, 62));
            g.drawLine(55, y, 1445, y);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 19));
            g.drawString("Subtotal: " + money(sale.getSubtotal()) + " COP", 65, y + 35);
            g.drawString("Descuento: " + money(sale.getDiscount()) + " COP", 65, y + 66);
            g.drawString("IVA total: " + money(sale.getTax()) + " COP", 65, y + 97);
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.drawString("TOTAL: " + money(sale.getTotal()) + " COP", 65, y + 140);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString("CUFE SIMULADO: " + sale.getSimulatedCufe(), 65, y + 180);
            g.drawString("QR de demostracion: muestra datos internos de esta venta.", 65, y + 215);
            g.drawString("Sin autorizacion, transmision ni validacion de la DIAN.", 65, y + 245);
            BitMatrix qr;
            try {
                qr =
                        new QRCodeWriter()
                                .encode(
                                        "DEMO|"
                                                + sale.getNumber()
                                                + "|"
                                                + sale.getSimulatedCufe()
                                                + "|"
                                                + money(sale.getTotal()),
                                        BarcodeFormat.QR_CODE,
                                        245,
                                        245);
            } catch (WriterException error) {
                throw new IOException("QR could not be generated", error);
            }
            int qrY = y + 22;
            g.setColor(Color.BLACK);
            for (int row = 0; row < qr.getHeight(); row++)
                for (int col = 0; col < qr.getWidth(); col++)
                    if (qr.get(col, row)) g.fillRect(1160 + col, qrY + row, 1, 1);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(canvas, "png", output))
                throw new IOException("PNG writer unavailable");
            return output.toByteArray();
        }
    }

    private String money(double n) {
        return String.format(Locale.US, "%.2f", n);
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
