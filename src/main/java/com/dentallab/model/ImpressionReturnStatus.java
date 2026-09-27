package com.dentallab.model;

/** 取模缺陷退回单状态。 */
public enum ImpressionReturnStatus {
    PENDING_REIMPRESSION("待诊所重取模"),
    REIMPRESSION_SUBMITTED("诊所已重新取模"),
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
