package com.test.server;

import com.test.server.nva.NvaFrameDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import org.fourthline.cling.model.message.Connection;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.StreamServer;
import org.fourthline.cling.transport.spi.UpnpStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

public class NvaStreamServerImpl implements StreamServer<NvaStreamServerConfiguration> {
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
    private NvaStreamServerConfiguration configuration;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

    protected int localPort;
    protected String hostAddress;

    protected ChannelFuture serverFuture = null;
    protected Channel serverChannel;

    public NvaStreamServerImpl(NvaStreamServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void init(InetAddress bindAddress, Router router) throws InitializationException {

        try {
            hostAddress = bindAddress.getHostAddress();
            localPort = getConfiguration().getListenPort();

            ServerBootstrap bootstrap = new ServerBootstrap();
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, router.getConfiguration().getStreamServerExecutorService());
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                            pipeline.addLast("upnpHandler", new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                                    System.out.println("http request........" + ctx.channel().localAddress() + "  " + msg.toString());
                                    if (shouldUpgradeToNVA(msg)) {
                                        //switch to NVA protocol
                                        switchProtocol(msg, pipeline);
                                    } else {
                                        msg.retain();
                                        router.received(new HttpMessageUpnpStream(router.getProtocolFactory(), ctx.channel(), msg));
                                    }
                                }
                            });
                        }
                    });
            serverFuture = bootstrap.bind(localPort);
        } catch (Exception e) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + e, e);
        }
    }



    private boolean shouldUpgradeToNVA(FullHttpRequest httpRequest) {
        if (httpRequest.protocolVersion().protocolName().equals("NVA")) {
            System.out.println("upgrade to nva....." + "method: " + httpRequest.method() + " uri:" + httpRequest.uri());
            return true;
        }
        return false;
    }

    private void switchProtocol(FullHttpRequest switchRequest, ChannelPipeline pipeline) {
        HttpResponse response = generateNvaResponse(switchRequest);
        System.out.println("nva response: " + response);
        pipeline.writeAndFlush(response);
        pipeline.remove(HttpServerCodec.class);
        pipeline.remove(HttpObjectAggregator.class);
        pipeline.remove("upnpHandler");
        pipeline.addLast(new NvaFrameDecoder());
    }

    /**
     *
     * SETUP /projection NVA/1.0
     * Session: {session}
     * NvaVersion: 1
     * Connection: Keep-Alive
     * UUID: {device-UUID}
     * User-Agent: Linux/3.0.0 UPnP/1.0 Platinum/1.0.5.13
     * Host: 192.168.1.223:9958
     *
     * NVA/1.0 200 OK
     * NvaVersion: 1
     * Session: {session}
     * Connection: Keep-Alive
     * UUID: {device-UUID}
     * Date: Sun, 02 Jan 2022 03:58:00 GMT
     * Content-Length: 0
     * Server: Linux/3.0.0, UPnP/1.0, Platinum/1.0.5.13
     * @param switchRequest
     * @return
     */
    private HttpResponse generateNvaResponse(FullHttpRequest switchRequest) {
        HttpResponse response = new DefaultFullHttpResponse(switchRequest.protocolVersion(), HttpResponseStatus.OK);
        String session = switchRequest.headers().get("Session");
        String version = switchRequest.headers().get("NvaVersion");
        String uuid = MyMediaRender.identity.getUdn().getIdentifierString();
        String server = "Linux/3.0.0, UPnP/1.0, Platinum/1.0.5.13";
        String date = dateFormat.format(new Date());
        response.headers().set("Session", session);
        response.headers().set("NvaVersion", version);
        response.headers().set("UUID", uuid);
        response.headers().set("Date", date);
        response.headers().set("Server", server);
        response.headers().set("Connection", "Keep-Alive");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, -1);
        return response;
    }


    class HttpMessageUpnpStream extends UpnpStream {
        private Channel channel;

        private FullHttpRequest httpMessage;

        public HttpMessageUpnpStream(ProtocolFactory protocolFactory, FullHttpRequest httpMessage) {
            super(protocolFactory);
            this.httpMessage = httpMessage;
        }

        public HttpMessageUpnpStream(ProtocolFactory protocolFactory, Channel channel, FullHttpRequest httpMessage) {
            super(protocolFactory);
            this.channel = channel;
            this.httpMessage = httpMessage;
        }

        @Override
        public void run() {
            StreamRequestMessage requestMessage =
                    new StreamRequestMessage(
                            UpnpRequest.Method.getByHttpName(httpMessage.method().name()),
                            URI.create(httpMessage.uri())
                    );
            HttpVersion version = httpMessage.protocolVersion();
            requestMessage.getOperation().setHttpMinorVersion(version.minorVersion());
            requestMessage.setConnection(createConnection());
            Map<String, List<String>> headers = new HashMap<>();
            for(Map.Entry<String, String> entry : httpMessage.headers().entries()) {
                if (!headers.containsKey(entry.getKey())) {
                    headers.put(entry.getKey(), new ArrayList<>());
                }
                List<String> valueList = headers.get(entry.getKey());
                valueList.add(entry.getValue());
            }
            requestMessage.setHeaders(new UpnpHeaders(headers));

            requestMessage.setBody(httpMessage.content().toString(Charset.defaultCharset()));

            StreamResponseMessage responseMessage = process(requestMessage);
            HttpResponse httpResponse;
            if (responseMessage != null) {
                if (responseMessage.hasBody()) {
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(responseMessage.getBodyBytes());
                    HttpResponseStatus status = HttpResponseStatus.valueOf(responseMessage.getOperation().getStatusCode());
                    httpResponse = new DefaultFullHttpResponse(version, status, byteBuf);
                    httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseMessage.getBodyBytes().length);
                    System.out.println("http response....." + httpResponse.status() + " " + responseMessage.getBodyString());
                } else {
                    HttpResponseStatus status = HttpResponseStatus.valueOf(responseMessage.getOperation().getStatusCode());
                    httpResponse = new DefaultFullHttpResponse(version, status);
                    httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, -1);
                    System.out.println("http response....." + httpResponse.status());

                }
            } else {
                httpResponse = new DefaultFullHttpResponse(version, HttpResponseStatus.NOT_FOUND);
            }
            httpMessage.release();
            channel.writeAndFlush(httpResponse);
        }

        Connection createConnection() {
            return new Connection() {
                @Override
                public boolean isOpen() {
                    return channel.isOpen();
                }

                @Override
                public InetAddress getRemoteAddress() {
                    InetSocketAddress socketAddress = (InetSocketAddress)channel.remoteAddress();
                    return socketAddress.getAddress();
                }

                @Override
                public InetAddress getLocalAddress() {
                    InetSocketAddress socketAddress = (InetSocketAddress)channel.localAddress();
                    return socketAddress.getAddress();
                }
            };
        }

    }

    @Override
    public int getPort() {
        return this.localPort;
    }

    @Override
    public void stop() {
        serverChannel.close();
    }

    @Override
    public NvaStreamServerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void run() {
        try {
            serverFuture.sync();
            serverChannel = serverFuture.channel();
            localPort = ((InetSocketAddress)serverChannel.localAddress()).getPort();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("started netty server........." + localPort);

    }
}
