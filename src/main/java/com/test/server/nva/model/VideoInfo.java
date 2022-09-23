package com.test.server.nva.model;

import java.util.ArrayList;
import java.util.List;

public class VideoInfo {
    private String url;
    private String format;
    private Integer duration;
    private Integer size;
    private Integer quality;

    private List<Format> supportedQuality;


    public VideoInfo(String url, String format, Integer duration, Integer size, Integer quality) {
        this.url = url;
        this.format = format;
        this.duration = duration;
        this.size = size;
        this.quality = quality;
        this.supportedQuality = new ArrayList<>();
    }

    public void addSupportFormat(Format format) {
        this.supportedQuality.add(format);
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

    public List<Format> getSupportedQuality() {
        return supportedQuality;
    }
}
