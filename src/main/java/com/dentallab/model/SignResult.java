package com.dentallab.model;

/** 签收结果，直接影响诊所试戴安排与结算扣罚。 */
public enum SignResult {
    GOOD("完好签收"),
    LATE("延误签收"),
    DAMAGED("破损签收");

    private final String label;

    SignResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
