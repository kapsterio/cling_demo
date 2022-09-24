package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.Map;

public class Pause extends CommandExecutor {

    public Pause(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        session.getController().pause();
        return null;
    }
}
