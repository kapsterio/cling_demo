package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.Map;

public abstract class CommandExecutor {
    protected NvaSession session;

    public CommandExecutor(NvaSession session) {
        this.session = session;
    }

    public abstract Map<String, Object> execute(Map<String, Object> payload);
}
