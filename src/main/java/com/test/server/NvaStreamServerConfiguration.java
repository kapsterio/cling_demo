package com.test.server;

import org.fourthline.cling.transport.spi.StreamServerConfiguration;

public class NvaStreamServerConfiguration implements StreamServerConfiguration {
    protected int listenPort = 0;


    public NvaStreamServerConfiguration(int listenPort) {
        this.listenPort = listenPort;
    }

    @Override
    public int getListenPort() {
        return listenPort;
    }
}
