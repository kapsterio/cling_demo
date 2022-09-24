package com.test.server.nva.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  * receive nva:NvaRequest{seq=1,
 *  * payload={proj_type=1, epId=0, mobileVersion=6780300,
 *  * seekTs=21, sessionId=f162a661-4517-4f9f-9dea-7cbc6c56f745,
 *  * oid=515780389, type=0, userDesireQn=64,
 *  * isOpen=true, seasonId=0, accessKey=6d1f23bfafbc74ba7d5cdbf47cb00891,
 *  * otype=0, autoNext=true, biz_id=0, userDesireSpeed=1,
 *  * aid=515780389, contentType=0, cid=840381348,
 *  * danmakuSwitchSave=false, desc=0},
 *  * version=1, type='Command',
 *  * name='Play'}
 */
public class VideoInfo {
    private String url;
    private String format;
    private Integer duration;
    private Integer size;
    private Integer quality;

    private Map<String, Format> supportedQuality;
    private String title;



    public VideoInfo(String url, String format, Integer duration, Integer size, Integer quality) {
        this.url = url;
        this.format = format;
        this.duration = duration;
        this.size = size;
        this.quality = quality;
        this.supportedQuality = new HashMap<>();
    }

    public void addSupportFormat(String qn, Format format) {
        this.supportedQuality.put(qn, format);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getQuality() {
        return quality;
    }

    public Map<String, Format> getSupportedQuality() {
        return supportedQuality;
    }
}
