package com.frescuratotal.erp.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {
    private static final Logger LOGGER = Logger.getLogger("FrescuraTotal");

    static {
        try {
            Files.createDirectories(Path.of("logs"));
            FileHandler file = new FileHandler("logs/erp.log", true);
            file.setFormatter(new SimpleFormatter());
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(file);
        } catch (IOException error) {
            System.err.println("Could not open logs/erp.log: " + error.getMessage());
        }
    }

    private AppLogger() {}

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void error(String message, Exception error) {
        LOGGER.log(Level.SEVERE, message, error);
    }
}
