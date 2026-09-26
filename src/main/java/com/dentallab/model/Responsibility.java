package com.dentallab.model;

/** 返工责任归属。 */
public enum Responsibility {
    PENDING("待判定"),
    CLINIC("诊所责任"),
    LAB("技工所责任"),
    SHARED("双方共担");

    private final String label;

    Responsibility(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
