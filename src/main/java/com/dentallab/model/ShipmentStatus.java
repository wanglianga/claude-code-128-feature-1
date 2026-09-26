package com.dentallab.model;

/** 配送单状态。 */
public enum ShipmentStatus {
    SHIPPED("运输中"),
    DELIVERED("已签收");

    private final String label;

    ShipmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
