package com.test.server;


import com.test.server.nva.NvaMediaController;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MediaPlayerController implements NvaMediaController {

    private final CallbackMediaPlayerComponent mediaPlayerComponent;

    private final JFrame frame;
    private final MediaPlayerStateChangeListener listener;

    public MediaPlayerController(MediaPlayerStateChangeListener listener) {
        this.listener = listener;
        frame = new JFrame("My First Media Player");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
                System.exit(0);
            }
        });

        mediaPlayerComponent = new CallbackMediaPlayerComponent();  // This is the only change
        frame.setContentPane(mediaPlayerComponent);

        frame.setVisible(true);
    }

    public void play() {
        mediaPlayerComponent.mediaPlayer().controls().play();
    };

    public void pause() {
        mediaPlayerComponent.mediaPlayer().controls().pause();
    }

    public void stop() {
        mediaPlayerComponent.mediaPlayer().controls().stop();
        mediaPlayerComponent.release();
        frame.dispose();
    }

    public void seek(long tsInMs) {
        mediaPlayerComponent.mediaPlayer().controls().setTime(tsInMs);
    }

    @Override
    public long currentPosition() {
        return mediaPlayerComponent.mediaPlayer().status().time();
    }

    @Override
    public int currentVolume() {
        int volume = mediaPlayerComponent.mediaPlayer().audio().volume();
        if (volume < 0) {
            return 50;
        } else {
            return volume / 2;
        }
    }

    @Override
    public void setVolume(int volume) {
        int actualVolume = volume * 2;
        mediaPlayerComponent.mediaPlayer().audio().setVolume(actualVolume);
    }

    public void prepare(String uri) {
        mediaPlayerComponent.mediaPlayer().media().play(uri);
        mediaPlayerComponent.mediaPlayer().media().parsing().parse();
        mediaPlayerComponent.mediaPlayer().events().addMediaEventListener(new MediaEventAdapter() {
            @Override
            public void mediaMetaChanged(Media media, Meta metaType) {
                System.out.println("meta changed " + metaType.toString());
            }

            @Override
            public void mediaDurationChanged(Media media, long newDuration) {
                System.out.println("duration changed " + newDuration);
                listener.onDurationKnow(newDuration);
            }

            @Override
            public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
                System.out.println("parsed changed :" + newStatus);
            }

            @Override
            public void mediaFreed(Media media, MediaRef mediaFreed) {
                System.out.println("media freed" +  mediaFreed);
            }

            @Override
            public void mediaStateChanged(Media media, State newState) {
                System.out.println("state changed" + newState);
                if (newState.equals(State.OPENING)) {
                    listener.onLoading();
                } else if (newState.equals(State.PLAYING)) {
                    listener.onPlay();
                } else if (newState.equals(State.PAUSED)) {
                    listener.onPause();
                } else if (newState.equals(State.STOPPED)) {
                    listener.onStop();
                }
            }
        });
    }


}
