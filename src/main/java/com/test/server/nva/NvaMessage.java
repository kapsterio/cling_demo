package com.test.server.nva;

import java.util.Map;

public class NvaMessage {
    protected int seq;

    protected Map<String, Object> payload;

    public NvaMessage(int seq) {
        this.seq = seq;
    }

    public int getSeq() {
        return seq;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
