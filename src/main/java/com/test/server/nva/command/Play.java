package com.test.server.nva.command;

import com.test.server.nva.NvaSession;
import com.test.server.nva.model.VideoID;
import com.test.server.nva.model.VideoInfo;
import com.test.server.utils.VideoInfoLoader;

import java.util.Map;

/**
 * receive nva:NvaRequest{seq=1,
 * payload={proj_type=1, epId=0, mobileVersion=6780300,
 * seekTs=21, sessionId=f162a661-4517-4f9f-9dea-7cbc6c56f745,
 * oid=515780389, type=0, userDesireQn=64,
 * isOpen=true, seasonId=0, accessKey=6d1f23bfafbc74ba7d5cdbf47cb00891,
 * otype=0, autoNext=true, biz_id=0, userDesireSpeed=1,
 * aid=515780389, contentType=0, cid=840381348,
 * danmakuSwitchSave=false, desc=0},
 * version=1, type='Command',
 * name='Play'}
 */
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
