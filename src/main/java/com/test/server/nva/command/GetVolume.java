package com.test.server.nva.command;

import com.test.server.nva.NvaSession;

import java.util.HashMap;
import java.util.Map;

public class GetVolume extends CommandExecutor {
    public GetVolume(NvaSession session) {
        super(session);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("volume", session.getController().currentVolume());
        return result;
    }
}
