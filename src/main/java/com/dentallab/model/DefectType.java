package com.dentallab.model;

/** 取模缺陷类型：技工所接收模型后发现的缺陷。 */
public enum DefectType {
    MARGIN_UNCLEAR("边缘不清"),
    BUBBLE("模型气泡");

    private final String label;

    DefectType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
