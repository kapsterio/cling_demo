package com.test.server.nva;

import com.test.server.MediaPlayerController;
import com.test.server.MediaPlayerStateChangeListener;
import com.test.server.nva.command.CommandExecutor;
import com.test.server.nva.command.Pause;
import com.test.server.nva.command.Play;
import com.test.server.nva.command.Resume;
import com.test.server.nva.command.Seek;
import com.test.server.nva.command.Stop;
import com.test.server.nva.command.SwitchQn;
import com.test.server.nva.model.Format;
import com.test.server.nva.model.VideoID;
import com.test.server.nva.model.VideoInfo;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NvaSession {
    private static final String TYPE_COMMAND = "Command";
    private String ssid;
    private String version;
    private int currentSeq = 1;
    private Channel channel;
    private List<NvaMessage> pendingMessages;

    private NvaMediaController controller;

    private Map<String, CommandExecutor> supportedCommands;

    private VideoInfo currentVideoInfo;
    private int currentQn;
    private String key;
    private VideoID currentVideoID;

    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private ScheduledFuture progressTrigger = null;


    public NvaSession(String ssid, String version, Channel channel) {
        this.ssid = ssid;
        this.version = version;
        this.channel = channel;
        this.pendingMessages = new ArrayList<>();
        this.controller = new MediaPlayerController(new MediaPlayerStateChangeListener() {
            @Override
            public void onDurationKnow(long duration) {
            }

            @Override
            public void onPlay() {
                sendPlayStateChange(4);
            }

            @Override
            public void onPause() {
                sendPlayStateChange(5);
            }

            @Override
            public void onStop() {
                sendPlayStateChange(6);
            }

            @Override
            public void onLoading() {
                sendPlayStateChange(3);
            }
        });
        initializeCommands(controller);
    }

    /**
     *             case "Play":
     *             case "PlayUrl":
     *             case "Pause":
     *             case "Resume":
     *             case "Stop":
     *             case "Seek":
     *             case "SwitchDanmaku":
     *             case "SwitchSpeed":
     *             case "SwitchQn":
     *             case "GetTVInfo":
     *             case "GetVolume":
     * @param mediaController
     * @return
     */
    private void initializeCommands(NvaMediaController mediaController) {
        System.out.println("init commands ........................");
        supportedCommands = new HashMap<>();
        supportedCommands.put("Play", new Play(this));
        supportedCommands.put("Pause", new Pause(this));
        supportedCommands.put("Resume", new Resume(this));
        supportedCommands.put("Seek", new Seek(this));
        supportedCommands.put("Stop", new Stop(this));
        supportedCommands.put("SwitchQn", new SwitchQn(this));
        //todo
    }

    public String getSsid() {
        return ssid;
    }

    public String getVersion() {
        return version;
    }

    public int getCurrentSeq() {
        return currentSeq;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public NvaMediaController getController() {
        return controller;
    }

    private int incrSeq() {
        return currentSeq ++;
    }

    public void setCurrentVideoInfo(VideoInfo currentVideoInfo) {
        this.currentVideoInfo = currentVideoInfo;
    }

    public int getCurrentQn() {
        return currentQn;
    }

    public void setCurrentQn(int currentQn) {
        this.currentQn = currentQn;
    }

    public VideoID getCurrentVideoID() {
        return currentVideoID;
    }

    public void setCurrentVideoID(VideoID currentVideoID) {
        this.currentVideoID = currentVideoID;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void channelRestore(Channel channel) {
        this.channel = channel;
        this.currentSeq = 1;
        this.pendingMessages.forEach(channel::writeAndFlush);
    }

    public void processMessage(NvaMessage message) {
        if (message instanceof NvaPing) {
            sendMessage(new NvaResponse(message.getSeq()));
        } else if (message instanceof NvaRequest) {
            processRequest((NvaRequest) message);
        } else {
            System.out.println("receive response......" + message);
        }
    }

    private void processRequest(NvaRequest request) {
        CommandExecutor commandExecutor = supportedCommands.get(request.getName());
        if (commandExecutor == null) {
            System.out.println("unsupported command: " + request.getName());
        } else {
            Map<String, Object> response = commandExecutor.execute(request.getPayload());
            if (response != null) {
                sendMessage(new NvaResponse(request.getSeq(), response));
            }
        }
    }



    private void sendMessage(NvaMessage message) {
        if (channel == null || !channel.isActive()) {
            pendingMessages.add(message);
        } else {
            channel.writeAndFlush(message);
        }
    }

    public void sendPing() {
        sendMessage(new NvaPing(incrSeq()));
    }

    public void sendPlayStateChange(int state) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("playState", state);
        sendMessage(new NvaRequest(incrSeq(), payload, Integer.parseInt(version), TYPE_COMMAND, "OnPlayState"));
    }



    public void sendDanmakuState(boolean isOpen) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("open", isOpen);
        sendMessage(new NvaRequest(incrSeq(), payload, Integer.parseInt(version), TYPE_COMMAND, "OnDanmakuSwitch"));
    }


    public void sendEpisodeState() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> item = new HashMap<>();
        item.put("aid", currentVideoID.getAid());
        item.put("cid", currentVideoID.getCid());
        item.put("contentType", currentVideoID.getContentType());
        item.put("epId", currentVideoID.getEpId());
        item.put("seasonId", currentVideoID.getSeasonId());
        payload.put("playItem", item);
        Map<String, Object> qnDesc = new HashMap<>();
        qnDesc.put("curQn", currentVideoInfo.getQuality());
        qnDesc.put("supportQnList", currentVideoInfo.getSupportedQuality().values().stream().map(Format::toMap).collect(Collectors.toList()));
        qnDesc.put("userDesireQn", currentQn);
        payload.put("qnDesc", qnDesc);
        payload.put("title", "xxxxxxxxxxxxxxxx");
        sendMessage(new NvaRequest(incrSeq(), payload, Integer.parseInt(version), TYPE_COMMAND, "OnEpisodeSwitch"));
    }

    public void sendQnState() {
        Map<String, Object> qnDesc = new HashMap<>();
        qnDesc.put("curQn", currentVideoInfo.getQuality());
        qnDesc.put("supportQnList", currentVideoInfo.getSupportedQuality().values().stream().map(Format::toMap).collect(Collectors.toList()));
        qnDesc.put("userDesireQn", currentQn);
        sendMessage(new NvaRequest(incrSeq(), qnDesc, Integer.parseInt(version), TYPE_COMMAND, "OnQnSwitch"));
    }

    public void sendSpeedState() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("currSpeed", 1.0);
        Double[] speeds  = new Double[] {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};
        List<Double> list = Arrays.asList(speeds);
        payload.put("supportSpeedList", list);
        sendMessage(new NvaRequest(incrSeq(), payload, Integer.parseInt(version), TYPE_COMMAND, "SpeedChanged"));
    }

    /**
     * {"duration": duration, "position": position}
     */
    public void sendProgressState() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("duration", currentVideoInfo.getDuration() / 1000);
        payload.put("position", controller.currentPosition() / 1000);
        sendMessage(new NvaRequest(incrSeq(), payload, Integer.parseInt(version), TYPE_COMMAND, "OnProgress"));
    }

    public void triggerProgressState() {
        progressTrigger = this.executorService.scheduleAtFixedRate(this::sendProgressState, 1, 1, TimeUnit.SECONDS);
    }

    public void cancelProgressTrigger() {
        if (progressTrigger != null) {
            progressTrigger.cancel(true);
        }
    }

    public void channelDown() {
        channel = null;
        cancelProgressTrigger();
    }
}
