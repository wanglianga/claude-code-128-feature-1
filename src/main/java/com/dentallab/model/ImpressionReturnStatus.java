package com.dentallab.model;

/** 取模缺陷退回单状态流转：已退回待重取 → 诊所已重取 → 诊所已确认 → 医生确认归档。 */
public enum ImpressionReturnStatus {
    REPORTED("已退回待诊所重取"),
    REIMPRESSED("诊所已重新取模"),
    CLINIC_CONFIRMED("诊所端已确认"),
    ARCHIVED("已归档");

    private final String label;

    ImpressionReturnStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
