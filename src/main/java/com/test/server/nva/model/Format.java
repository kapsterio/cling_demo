package com.test.server.nva.model;

import java.util.HashMap;
import java.util.Map;

public class Format {
    private int qn;
    private boolean needVip;
    private boolean needLogin;
    private String format;
    private String desc;
    private String displayDesc;
    private String superScript;


    public Format(String format, String desc, String displayDesc, String superScript) {

        this.format = format;
        this.desc = desc;
        this.displayDesc = displayDesc;
        this.superScript = superScript;
    }

    public void setQn(int qn) {
        this.qn = qn;
    }

    public void setNeedVip(boolean needVip) {
        this.needVip = needVip;
    }

    public void setNeedLogin(boolean needLogin) {
        this.needLogin = needLogin;
    }

    public int getQn() {
        return qn;
    }

    public boolean isNeedVip() {
        return needVip;
    }

    public boolean isNeedLogin() {
        return needLogin;
    }

    public String getFormat() {
        return format;
    }

    public String getDesc() {
        return desc;
    }

    public String getDisplayDesc() {
        return displayDesc;
    }

    public String getSuperScript() {
        return superScript;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("description", desc);
        map.put("displayDesc", displayDesc);
        map.put("superscript", superScript);
        map.put("quality", qn);
        map.put("needVip", needVip);
        map.put("needLogin", needLogin);
        return map;
    }
}
