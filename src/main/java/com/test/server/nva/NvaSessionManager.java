package com.test.server.nva;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

public class NvaSessionManager {
    public static AttributeKey<String> SSID = AttributeKey.valueOf("ssid");
    private Map<String, NvaSession> sessions = new HashMap<>();

    public void setupSession(String ssid, String version, Channel channel) {
        NvaSession session = new NvaSession(ssid, version, channel);
        sessions.put(ssid, session);
        channel.attr(SSID).set(ssid);
    }

    public void restoreSession(String ssid, Channel channel) {
        NvaSession session = sessions.get(ssid);
        if (session != null) {
            session.channelRestore(channel);
        } else {
            throw new RuntimeException(ssid + " not exist");
        }
        //todo send some thing;
    }

    public void removeSession(String ssid) {
        this.sessions.remove(ssid);
    }

    public NvaSession getSession(String ssid) {
        return sessions.get(ssid);
    }
}
