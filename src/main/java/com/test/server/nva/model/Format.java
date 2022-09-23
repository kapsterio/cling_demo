package com.test.server.nva.model;

public class Format {
    private Integer qn;
    private String format;
    private String desc;

    public Format(Integer qn, String format, String desc) {
        this.qn = qn;
        this.format = format;
        this.desc = desc;
    }

    public Integer getQn() {
        return qn;
    }

    public String getFormat() {
        return format;
    }

    public String getDesc() {
        return desc;
    }
}
