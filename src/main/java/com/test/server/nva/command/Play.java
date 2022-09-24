package com.test.server.nva.command;

import com.test.server.nva.NvaSession;
import com.test.server.nva.model.VideoID;
import com.test.server.nva.model.VideoInfo;
import com.test.server.utils.VideoInfoLoader;

import java.util.Map;

public class Play extends CommandExecutor {
    public Play(NvaSession session) {
        super(session);
    }


    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        int cid = (Integer) payload.get("cid");
        int aid = (Integer) payload.get("aid");
        int epid = (Integer) payload.get("epId");
        int seasonId = (Integer) payload.get("seasonId");
        int contentType = (Integer) payload.get("contentType");
        int oid = (Integer) payload.get("oid");
        VideoID videoId = new VideoID(cid, contentType, aid, epid, seasonId, oid);
        session.setCurrentVideoID(videoId);

        int qn = (Integer) payload.get("userDesireQn");
        session.setCurrentQn(qn);

        String key = String.valueOf(payload.get("accessKey"));
        session.setKey(key);

        try {
            VideoInfo videoInfo = VideoInfoLoader.loadVideoInfo(cid, epid, oid, qn, key);
            if (videoInfo == null) {
                System.out.println("fail to play .......");
                return null;
            }
            session.setCurrentVideoInfo(videoInfo);
            session.getController().prepare(videoInfo.getUrl());
            Integer tsInSecond = (Integer) payload.getOrDefault("seekTs", 0);
            session.getController().seek(tsInSecond * 1000);
            session.sendDanmakuState(false);
            session.sendEpisodeState();
            session.sendQnState();
            session.sendSpeedState();
            session.triggerProgressState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
