package com.test.server.nva;

import java.util.Map;

public class NvaResponse extends NvaMessage {
    public NvaResponse(int seq) {
        super(seq);
    }

    public NvaResponse(int seq, Map<String, Object> payload) {
        super(seq, payload);
    }

    @Override
    public String toString() {
        return "NvaResponse{" +
                "seq=" + seq +
                ", payload=" + payload +
                '}';
    }
}
