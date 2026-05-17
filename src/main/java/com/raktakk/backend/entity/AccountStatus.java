package com.raktakk.backend.entity;

public enum AccountStatus {
    ACTIVE("active"),
    PENDING("pending"),
    SUSPENDED("suspended"),
    INACTIVE("inactive");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AccountStatus fromValue(String value) {
        for (AccountStatus status : AccountStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown account status: " + value);
    }
}
