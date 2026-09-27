package com.dentallab.model;

/** 多端同步通知的接收端。 */
public enum RecipientType {
    PATIENT("患者端"),
    DOCTOR("医生端"),
    CLINIC("诊所端");

    private final String label;

    RecipientType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
