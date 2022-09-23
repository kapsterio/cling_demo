package com.test.server.nva;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class NvaMessageHandler extends SimpleChannelInboundHandler<NvaMessage> {
    private NvaSessionManager sessionManager;

    public NvaMessageHandler(NvaSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NvaMessage msg) throws Exception {
        getSession(ctx).processMessage(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                getSession(ctx).sendPing();
            } else if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        getSession(ctx).channelDown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private NvaSession getSession(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String ssid = channel.attr(NvaSessionManager.SSID).get();
        return sessionManager.getSession(ssid);
    }
}
