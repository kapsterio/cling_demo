package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.Map;

public class Seek extends CommandExecutor {

    public Seek(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        int ts = (Integer) payload.get("seekTs");
        session.getController().seek(ts * 1000L);
        return null;
    }
}
