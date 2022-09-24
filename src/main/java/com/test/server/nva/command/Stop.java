package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.Map;

public class Stop extends CommandExecutor {

    public Stop(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        session.getController().stop();
        session.cancelProgressTrigger();
        return null;
    }
}
