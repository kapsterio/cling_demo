package com.test.server.nva;

public interface NvaMediaController {

    void prepare(String uri);

    void play();

    void pause();

    void stop();

    void seek(long tsInMs);



}
