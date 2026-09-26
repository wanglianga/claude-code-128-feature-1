package com.dentallab.model;

/** 生产工序，按枚举声明顺序流转。 */
public enum StageType {
    MODEL_RECEIVING("模型接收"),
    SCAN_DESIGN("扫描设计"),
    DOCTOR_CONFIRM("医生确认"),
    TEETH_ARRANGEMENT("排牙"),
    SINTERING("烧结"),
    POLISHING("抛光"),
    QC("质检"),
    DELIVERY("配送");

    private final String label;

    StageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 下一道工序，最后一道返回 null。 */
    public StageType next() {
        StageType[] values = values();
        int next = ordinal() + 1;
        return next < values.length ? values[next] : null;
    }
}
