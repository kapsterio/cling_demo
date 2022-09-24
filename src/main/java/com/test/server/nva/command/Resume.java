package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.Map;

public class Resume extends CommandExecutor {

    public Resume(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        session.getController().play();
        return null;
    }
}
