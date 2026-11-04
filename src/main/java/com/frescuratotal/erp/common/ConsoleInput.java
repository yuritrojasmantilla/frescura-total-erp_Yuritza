package com.frescuratotal.erp.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String text(String prompt) {
        System.out.print(prompt + ": ");
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Input closed");
        return scanner.nextLine().trim();
    }

    public String required(String prompt) {
        while (true) {
            String input = text(prompt);
            if (!input.isBlank()) return input;
            System.out.println("A value is required.");
        }
    }

    public int integer(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(text(prompt));
            } catch (NumberFormatException error) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    public double decimal(String prompt) {
        while (true) {
            try {
                double value = Double.parseDouble(text(prompt + " (use a decimal point)"));
                if (Double.isFinite(value)) return value;
            } catch (NumberFormatException error) {
            }
            System.out.println("Enter a finite number, for example 12.50.");
        }
    }

    public LocalDate date(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(text(prompt + " (YYYY-MM-DD)"));
            } catch (DateTimeParseException error) {
                System.out.println("Enter a valid date, for example 2026-11-04.");
            }
        }
    }
}
