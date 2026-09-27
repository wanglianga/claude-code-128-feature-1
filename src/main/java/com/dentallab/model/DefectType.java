package com.dentallab.model;

/** 取模缺陷类型。 */
public enum DefectType {
    MARGIN_UNCLEAR("边缘不清"),
    BUBBLE("气泡"),
    OTHER("其他");

    private final String label;

    DefectType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
