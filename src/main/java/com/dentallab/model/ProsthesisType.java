package com.dentallab.model;

/** 修复体类型。 */
public enum ProsthesisType {
    PFM_CROWN("烤瓷冠"),
    ALL_CERAMIC_CROWN("全瓷冠"),
    REMOVABLE_DENTURE("活动义齿"),
    FLEXIBLE_DENTURE("隐形义齿"),
    IMPLANT_RESTORATION("种植上部修复");

    private final String label;

    ProsthesisType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
