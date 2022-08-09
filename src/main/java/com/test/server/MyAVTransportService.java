package com.test.server;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.avtransport.AbstractAVTransportService;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.RecordQualityMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.TransportStatus;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MyAVTransportService extends AbstractAVTransportService {
    private DocumentBuilder builder;
    private String title;
    private String uri;
    private String metadata;
    private String duration;
    private String relativeTimePosition;
    private String absoluteTimePosition;

    private TransportState state = TransportState.NO_MEDIA_PRESENT;
    private TransportStatus status;
    private String speed;

    private PlayMode playMode;


    private MediaPlayerStateChangeListener listener;
    private MediaPlayerController mediaPlayer;

    public MyAVTransportService() {
        super();
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAVTransportURI(UnsignedIntegerFourBytes instanceId, String currentURI, String currentURIMetaData) throws AVTransportException {
        this.uri = currentURI;
        this.metadata = currentURIMetaData;
        this.state = TransportState.PLAYING;
        this.status = TransportStatus.OK;
        this.speed = "1";
        this.relativeTimePosition = "00:00:00";
        this.absoluteTimePosition = "00:00:00";
        System.out.println("URI: " + currentURI);
        System.out.println("meta data" + currentURIMetaData);
        try {
            Document doc = builder.parse(new ByteArrayInputStream(currentURIMetaData.getBytes(StandardCharsets.UTF_8)));
            this.title = doc.getDocumentElement().getElementsByTagName("dc:title").item(0).getTextContent();
            System.out.println("title: " + title);
        } catch (Exception e) {
            e.printStackTrace();
        }

        listener = new MediaPlayerStateChangeListener() {
            @Override
            public void onDurationKnow(long durationInMs) {
                long durationInSecond = durationInMs / 1000;
                long hour = durationInSecond / 3600;
                long minutes = durationInSecond % 3600 / 60;
                long second = durationInSecond % 60;

                duration = String.format("%d:%02d:%02d", hour, minutes, second);
                System.out.println("duration " + duration);
            }

            @Override
            public void onPlay() {

            }

            @Override
            public void onPause() {

            }

            @Override
            public void onStop() {

            }

            @Override
            public void onSeek(long time) {

            }
        };
        mediaPlayer = new MediaPlayerController(listener);
        mediaPlayer.prepare(uri);
    }

    @Override
    public void setNextAVTransportURI(UnsignedIntegerFourBytes instanceId, String nextURI, String nextURIMetaData) throws AVTransportException {
    }

    @Override
    public MediaInfo getMediaInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("get media info ....");
        return new MediaInfo(this.uri, this.metadata, new UnsignedIntegerFourBytes(1), duration, StorageMedium.NONE);
    }

    @Override
    public TransportInfo getTransportInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("get transport info .....");
        return new TransportInfo(state, status, speed);
    }

    @Override
    public PositionInfo getPositionInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("get position info .....");
        return new PositionInfo(1, duration, uri, relativeTimePosition, absoluteTimePosition);
    }

    @Override
    public DeviceCapabilities getDeviceCapabilities(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("get device capabilities...");
        return null;
    }

    @Override
    public TransportSettings getTransportSettings(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("get transport settings....");
        return new TransportSettings(playMode, RecordQualityMode.NOT_IMPLEMENTED);
    }

    @Override
    public void stop(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        this.state = TransportState.STOPPED;
        this.mediaPlayer.stop();
        System.out.println("media  stopped.");
    }

    @Override
    public void play(UnsignedIntegerFourBytes instanceId, String speed) throws AVTransportException {
        this.state = TransportState.PLAYING;
        this.mediaPlayer.play();
        System.out.println("begin to play....");
    }

    @Override
    public void pause(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        this.state = TransportState.PAUSED_PLAYBACK;
        this.mediaPlayer.pause();
        System.out.println("media paused ...");
    }

    @Override
    public void record(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("media recoding ...");

    }

    @Override
    public void seek(UnsignedIntegerFourBytes instanceId, String unit, String target) throws AVTransportException {
        System.out.println("seek to target:" + target + " unit :" + unit);
        this.relativeTimePosition = target;
        this.absoluteTimePosition = target;
        this.state = TransportState.TRANSITIONING;

        String[] arr = target.split(":");
        int hour = Integer.parseInt(arr[0]);
        int minutes = Integer.parseInt(arr[1]);
        int seconds = Integer.parseInt(arr[2]);
        long timeInMs = (hour * 3600L + minutes * 60L + seconds) * 1000;
        this.mediaPlayer.seek(timeInMs);
    }

    @Override
    public void next(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("next ...");
    }

    @Override
    public void previous(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
        System.out.println("previous ...");

    }

    @Override
    public void setPlayMode(UnsignedIntegerFourBytes instanceId, String newPlayMode) throws AVTransportException {
        System.out.println("set play mode: " + newPlayMode);
        this.playMode = PlayMode.valueOf(newPlayMode);
    }

    @Override
    public void setRecordQualityMode(UnsignedIntegerFourBytes instanceId, String newRecordQualityMode) throws AVTransportException {
        System.out.println("set quality mode" + newRecordQualityMode);
    }

    @Override
    protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) throws Exception {
        return new TransportAction[]{TransportAction.Play, TransportAction.Pause, TransportAction.Stop,
                TransportAction.Seek};
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        return new UnsignedIntegerFourBytes[] {new UnsignedIntegerFourBytes(1)};
    }
}
