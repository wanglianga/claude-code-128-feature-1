package com.dentallab.model;

/** 异常处理状态。 */
public enum ExceptionStatus {
    OPEN("待处理"),
    PROCESSING("处理中"),
    RESOLVED("已解决");

    private final String label;

    ExceptionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
