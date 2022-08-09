package com.test.server;


import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MediaPlayerController {

    private final EmbeddedMediaPlayer mediaPlayerComponent;

    private final JFrame frame;
    private final MediaPlayerStateChangeListener listener;

    public MediaPlayerController(MediaPlayerStateChangeListener listener) {
        this.listener = listener;
        frame = new JFrame("demo media player");
        frame.setBounds(100, 100, 600, 400);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
                System.exit(0);
            }
        });

        final MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
        mediaPlayerComponent = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        Window window = new Window(frame);
        window.setBackground(Color.gray);
        VideoSurface videoSurface = mediaPlayerFactory.videoSurfaces().newVideoSurface(window);
        mediaPlayerComponent.videoSurface().set(videoSurface);

        window.setBounds(100, 100, 600, 400);
        window.setIgnoreRepaint(true);

        frame.setVisible(true);
        window.setVisible(true);
    }

    public void play() {
        mediaPlayerComponent.controls().play();
    };

    public void pause() {
        mediaPlayerComponent.controls().pause();
    }

    public void stop() {
        mediaPlayerComponent.controls().stop();
    }

    public void seek(long tsInMs) {
        mediaPlayerComponent.controls().setTime(tsInMs);
    }

    public void prepare(String uri) {
        mediaPlayerComponent.media().play(uri);
        mediaPlayerComponent.media().parsing().parse();
        mediaPlayerComponent.events().addMediaEventListener(new MediaEventAdapter() {
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
            }
        });
    }


}
