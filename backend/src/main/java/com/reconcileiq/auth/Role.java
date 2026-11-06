package com.reconcileiq.auth;

public enum Role {
    ROLE_FINANCE_EXECUTIVE,
    ROLE_COFOUNDER;

    public String toAuthority() {
        return name();
    }
}
