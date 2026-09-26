package com.dentallab.model;

/** 返工单状态。 */
public enum ReworkStatus {
    OPEN("返工中"),
    COMPLETED("返工完成");

    private final String label;

    ReworkStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
