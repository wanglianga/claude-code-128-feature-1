package com.dentallab.model;

/** 通知类型。 */
public enum NoticeType {
    REIMPRESSION_REQUEST("通知诊所重取模"),
    APPOINTMENT_ADJUSTMENT("患者预约调整提醒"),
    DELIVERY_DATE_SYNC("医生端交付日期同步");

    private final String label;

    NoticeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
