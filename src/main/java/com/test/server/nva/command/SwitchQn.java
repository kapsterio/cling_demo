package com.test.server.nva.command;

import com.test.server.nva.NvaSession;
import com.test.server.nva.model.Format;
import com.test.server.nva.model.VideoID;
import com.test.server.nva.model.VideoInfo;
import com.test.server.utils.MapUtil;
import com.test.server.utils.VideoInfoLoader;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
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
public class SwitchQn extends CommandExecutor {

    public SwitchQn(NvaSession session) {
        super(session);
    }


    @Override
    public Map<String, Object> execute(Map<String, Object> payload) {
        int newQn = (Integer) payload.get("qn");
        session.setCurrentQn(newQn);
        VideoID videoID = session.getCurrentVideoID();

        try {
            VideoInfo videoInfo = VideoInfoLoader.loadVideoInfo(
                    videoID.getCid(), videoID.getEpId(), videoID.getOid(),
                    newQn, session.getKey());
            session.setCurrentVideoInfo(videoInfo);
            session.getController().prepare(videoInfo.getUrl());
            long position = session.getController().currentPosition();
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
