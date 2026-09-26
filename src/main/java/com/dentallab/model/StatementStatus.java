package com.dentallab.model;

/** 诊所月结单状态。 */
public enum StatementStatus {
    GENERATED("已生成待确认"),
    CONFIRMED("诊所已确认");

    private final String label;

    StatementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
