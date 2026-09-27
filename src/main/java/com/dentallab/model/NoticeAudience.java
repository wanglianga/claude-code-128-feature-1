package com.dentallab.model;

/** 通知接收端。 */
public enum NoticeAudience {
    CLINIC("诊所端"),
    DOCTOR("医生端"),
    PATIENT("患者端");

    private final String label;

    NoticeAudience(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
