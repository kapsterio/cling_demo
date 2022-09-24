package com.test.server.nva;

import java.util.Map;

public class NvaRequest extends NvaMessage {
    private int version;
    private String type;
    private String name;

    public NvaRequest(int seq, int version, String type, String name) {
        super(seq);
        this.version = version;
        this.type = type;
        this.name = name;
    }

    public NvaRequest(int seq, Map<String, Object> payload, int version, String type, String name) {
        super(seq, payload);
        this.version = version;
        this.type = type;
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "NvaRequest{" +
                "seq=" + seq +
                ", payload=" + payload +
                ", version=" + version +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
