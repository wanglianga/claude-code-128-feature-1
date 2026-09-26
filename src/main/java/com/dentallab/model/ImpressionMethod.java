package com.dentallab.model;

/** 取模方式。 */
public enum ImpressionMethod {
    TRADITIONAL("传统印模"),
    DIGITAL_SCAN("数字口扫");

    private final String label;

    ImpressionMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
