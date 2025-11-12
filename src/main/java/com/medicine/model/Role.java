package com.medicine.model;

public enum Role {
    ADMIN("관리자"),
    FATHER("아버지"),
    FAMILY("가족"),
    OTHER("기타");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
