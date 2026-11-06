package com.reconcileiq.common;

public final class PiiMasker {

    private PiiMasker() {}

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        String[] parts = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            String part = parts[i];
            if (part.length() <= 1) {
                result.append("*");
            } else {
                result.append(part.charAt(0)).append("*".repeat(Math.max(0, part.length() - 1)));
            }
        }
        return result.toString();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return phone;
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    public static String maskAddress(String address) {
        if (address == null || address.length() < 3) return address;
        return address.substring(0, 3) + "*".repeat(Math.max(0, address.length() - 3));
    }
}