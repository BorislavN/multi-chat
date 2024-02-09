package app.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public static void log(String message) {
        System.out.printf("[%s]: \"%s\"%n", getTime(), message);
    }

    public static void logError(String message, Throwable error) {
        System.err.printf("[%s]: %s - %s%n", getTime(), message, error.getMessage());
    }

    public static void logAsError(String message) {
        System.err.printf("[%s]: %s%n", getTime(), message);
    }

    private static String getTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        return formatter.format(LocalDateTime.now());
    }
}
