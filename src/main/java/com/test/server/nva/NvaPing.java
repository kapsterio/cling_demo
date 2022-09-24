package com.test.server.nva;

public class NvaPing extends NvaMessage {
    public NvaPing(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return "NvaPing{" +
                "seq=" + seq +
                ", payload=" + payload +
                '}';
    }
}
