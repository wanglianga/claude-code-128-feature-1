package com.dentallab.model;

/** 订单整体状态。 */
public enum OrderStatus {
    SUBMITTED("已提交"),
    ACCEPTED("技工所已接单"),
    IN_PRODUCTION("生产中"),
    QC_PASSED("质检通过"),
    SHIPPED("已发货"),
    DELIVERED("已签收"),
    TRY_IN("试戴中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
