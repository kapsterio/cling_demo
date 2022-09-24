package com.test.server;

public interface MediaPlayerStateChangeListener {
    void onDurationKnow(long duration);
    void onPlay();
    void onPause();
    void onStop();
    default void onLoading() {

    }
}
