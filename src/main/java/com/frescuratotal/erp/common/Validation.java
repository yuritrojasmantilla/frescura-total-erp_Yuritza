package com.frescuratotal.erp.common;

public final class Validation {
    private Validation() {}

    public static String text(String value, String label, int maximum) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(label + " is required");
        String clean = value.trim();
        if (clean.length() > maximum) throw new IllegalArgumentException(label + " is too long");
        return clean;
    }

    public static double decimal(double value, String label, boolean positive) {
        if (!Double.isFinite(value) || value < 0 || (positive && value == 0) || value > 99999999.99)
            throw new IllegalArgumentException(
                    label
                            + " must be a "
                            + (positive ? "positive" : "non-negative")
                            + " finite number up to 99999999.99");
        if (Math.abs(value * 100 - Math.rint(value * 100)) > 0.000001)
            throw new IllegalArgumentException(label + " accepts at most two decimal places");
        return Math.round(value * 100.0) / 100.0;
    }

    public static int positiveId(int value, String label) {
        if (value <= 0) throw new IllegalArgumentException(label + " must be positive");
        return value;
    }
}
