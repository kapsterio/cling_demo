package com.test.server.nva;

import com.test.server.MediaPlayerController;
import com.test.server.MediaPlayerStateChangeListener;
import com.test.server.nva.command.CommandExecutor;
import com.test.server.nva.command.Play;
import com.test.server.nva.model.VideoInfo;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NvaSession {
    private String ssid;
    private String version;
    private int currentSeq = 0;
    private Channel channel;
    private List<NvaMessage> pendingMessages;

    private NvaMediaController controller;

    private Map<String, CommandExecutor> supportedCommands;

    private VideoInfo currentVideoInfo;

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

            }

            @Override
            public void onPause() {

            }

            @Override
            public void onStop() {

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

    public NvaMediaController getController() {
        return controller;
    }

    private int incrSeq() {
        return currentSeq ++;
    }

    public void setCurrentVideoInfo(VideoInfo currentVideoInfo) {
        this.currentVideoInfo = currentVideoInfo;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void channelRestore(Channel channel) {
        this.channel = channel;
        this.currentSeq = 0;
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

    public void channelDown() {
        channel = null;
    }
}
