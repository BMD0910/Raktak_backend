package com.raktakk.backend.entity;

public enum ServiceStatus {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    INACTIVE("inactive");

    private final String value;

    ServiceStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ServiceStatus fromValue(String value) {
        for (ServiceStatus status : ServiceStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown service status: " + value);
    }
}
