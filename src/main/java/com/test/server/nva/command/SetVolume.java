package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.HashMap;
import java.util.Map;

public class SetVolume extends CommandExecutor {

    public SetVolume(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        int volume = (Integer) payload.get("volume");
        session.getController().setVolume(volume);
        return null;
    }
}
