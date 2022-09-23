package com.test.server.nva;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

import static com.test.server.nva.NvaFrameDecoder.FLAG_PING;
import static com.test.server.nva.NvaFrameDecoder.FLAG_REQUEST;
import static com.test.server.nva.NvaFrameDecoder.FLAG_RESPONSE;

public class NvaFrameEncoder extends MessageToByteEncoder<NvaMessage> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, NvaMessage msg, ByteBuf out) throws Exception {
        if (msg instanceof NvaRequest) {
            encodeRequest((NvaRequest)msg, out);
        } else if (msg instanceof  NvaResponse) {
            encodeResponse((NvaResponse)msg, out);
        } else {
            encodePing((NvaPing) msg, out);
        }
    }

    private void encodePing(NvaPing msg, ByteBuf out) {
        out.writeByte(FLAG_PING);
        out.writeByte(0x00);
        out.writeInt(msg.getSeq());
        System.out.println("encode ping.... ");
    }

    private void encodeResponse(NvaResponse msg, ByteBuf out) throws Exception {
        out.writeByte(FLAG_RESPONSE);
        out.writeByte(msg.hasPayload() ? 1 : 0);
        out.writeInt(msg.getSeq());
        if (msg.hasPayload()) {
            String payloadJson = objectMapper.writeValueAsString(msg.getPayload());
            byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);
            out.writeInt(payload.length);
            out.writeBytes(payload);
        }
        System.out.println("encode response.... ");
    }

    private void encodeRequest(NvaRequest msg, ByteBuf out) throws Exception{
        out.writeByte(FLAG_REQUEST);
        out.writeByte(msg.hasPayload() ? 3 : 2);
        out.writeInt(msg.getSeq());
        out.writeByte(7);
        out.writeBytes("Command".getBytes(StandardCharsets.UTF_8));
        byte[] name = msg.getName().getBytes(StandardCharsets.UTF_8);
        out.writeByte(name.length);
        out.writeBytes(name);
        if (msg.hasPayload()) {
            String payloadJson = objectMapper.writeValueAsString(msg.getPayload());
            byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);
            out.writeInt(payload.length);
            out.writeBytes(payload);
        }
        System.out.println("encode request.... ");
    }
}
