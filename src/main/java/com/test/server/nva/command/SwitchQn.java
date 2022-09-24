package com.test.server.nva.command;

import com.test.server.nva.NvaSession;
import com.test.server.nva.model.VideoID;
import com.test.server.nva.model.VideoInfo;
import com.test.server.utils.VideoInfoLoader;

import java.util.Map;

public class SwitchQn extends CommandExecutor {

    public SwitchQn(NvaSession session) {
        super(session);
    }


    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        int newQn = (Integer) payload.get("qn");
        session.setCurrentQn(newQn);
        VideoID videoID = session.getCurrentVideoID();
        long position = session.getController().currentPosition();
        try {
            VideoInfo videoInfo = VideoInfoLoader.loadVideoInfo(
                    videoID.getCid(), videoID.getEpId(), videoID.getOid(),
                    newQn, session.getKey());
            if (videoInfo == null) {
                System.out.println("fail to switch .......");
                return null;
            }
            session.setCurrentVideoInfo(videoInfo);
            session.getController().prepare(videoInfo.getUrl());
            session.getController().seek(position);
            session.sendDanmakuState(false);
            session.sendEpisodeState();
            session.sendQnState();
            session.sendSpeedState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
