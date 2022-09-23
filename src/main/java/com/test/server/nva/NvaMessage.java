package com.test.server.nva;

import java.util.Map;

public class NvaMessage {
    protected int seq;

    protected Map<String, Object> payload;

    public NvaMessage(int seq) {
        this.seq = seq;
    }

    public NvaMessage(int seq, Map<String, Object> payload) {
        this.seq = seq;
        this.payload = payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getSeq() {
        return seq;
    }

    public boolean hasPayload() {
        return payload != null && !payload.isEmpty();
    }
    public Map<String, Object> getPayload() {
        return payload;
    }
}
