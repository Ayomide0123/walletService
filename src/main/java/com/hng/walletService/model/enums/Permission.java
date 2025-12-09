package com.hng.walletService.model.enums;

public enum Permission {
    DEPOSIT("deposit"),
    TRANSFER("transfer"),
    READ("read");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Permission fromValue(String value) {
        for (Permission permission : Permission.values()) {
            if (permission.value.equalsIgnoreCase(value)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Invalid permission: " + value);
    }
}
