package com.test.server.nva;

import java.util.HashMap;
import java.util.Map;

public class NvaSessionManager {
    private Map<String, NvaSession> sessions = new HashMap<>();

    public void addSession(NvaSession session) {
        this.sessions.put(session.getSsid(), session);
    }

    public void removeSession(String ssid) {
        this.sessions.remove(ssid);
    }

    public NvaSession getSession(String ssid) {
        return this.sessions.get(ssid);
    }
}
