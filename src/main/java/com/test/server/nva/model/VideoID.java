package com.test.server.nva.model;

public class VideoID {
    private int cid;
    private int contentType;
    private int aid;
    private int epId;
    private int seasonId;
    private int oid;

    public VideoID(int cid, int contentType, int aid, int epId, int seasonId, int oid) {
        this.cid = cid;
        this.contentType = contentType;
        this.aid = aid;
        this.epId = epId;
        this.seasonId = seasonId;
        this.oid = oid;
    }

    public int getCid() {
        return cid;
    }

    public int getContentType() {
        return contentType;
    }

    public int getAid() {
        return aid;
    }

    public int getEpId() {
        return epId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public int getOid() {
        return oid;
    }
}
