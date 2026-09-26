package com.dentallab.model;

/** 异常事件类型。 */
public enum ExceptionType {
    IMPRESSION_BUBBLE("取模气泡"),
    SHADE_DISPUTE("色号争议"),
    DESIGN_CHANGE("医生临时改设计"),
    SCHEDULE_CONGESTION("技工所排期拥堵"),
    TRY_IN_MISFIT("试戴不密合"),
    RUSH_REQUEST("患者加急"),
    DELIVERY_DELAY("配送延误"),
    DELIVERY_DAMAGE("配送破损"),
    REWORK_DISPUTE("返工责任不清");

    private final String label;

    ExceptionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
