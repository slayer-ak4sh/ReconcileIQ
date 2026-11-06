package com.reconcileiq.common;

public class PiiMasker {

    public static String maskName(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        if (trimmed.length() <= 1) return "*";
        return trimmed.charAt(0) + "*".repeat(Math.min(trimmed.length() - 1, 8));
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return "*".repeat(digits.length());
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) return null;
        if (address.length() <= 3) return "***";
        return address.substring(0, 3) + "***";
    }
}
