package com.test.server.nva;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * request
 * 0xe0 num_of_params:1 seq:4 version:1 length_of_type:1 type: length_of_type length_of_name:1
 * name:length_of_type  [length_of_payload:4 payload:length_of_payload]
 *
 * response
 * 0xc0 num_of_params:1(0/1) seq:4 [length_of_payload:4 payload: length_of_payload]
 *
 * ping:
 * 0xe4 num_of_params:1(0x00) seq:4
 *
 */
public class NvaFrameDecoder extends ByteToMessageDecoder {
    public final static int FLAG_REQUEST = 0xe0;
    public final static int FLAG_RESPONSE = 0xc0;
    public final static int FLAG_PING = 0xe4;

    private static TypeReference<HashMap<String, Object>> typeRef
            = new TypeReference<HashMap<String, Object>>() {};
    private static ObjectMapper mapper = new ObjectMapper();

    private final static int STATE_INITIAL = 0;
    private final static int STATE_READ_HEADER = 1;
    private final static int STATE_READ_NAME = 2;
    private final static int STATE_READ_PAYLOAD = 3;
    private final static int STATE_READ_PAYLOAD_LENGTH = 4;

    private int state = STATE_INITIAL;
    private int flag;
    private boolean hasPayload;
    private int numBytesToWait;


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("nav decoder added ..........");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case STATE_INITIAL:
                in.markReaderIndex();
                flag = in.readByte();
                state = STATE_READ_HEADER;
                if ((flag & 0xff) == FLAG_REQUEST) {
                    numBytesToWait = 15;
                    readRequestHeader(in, out);
                } else if ((flag & 0xff) == FLAG_RESPONSE){
                    numBytesToWait = 5;
                    readResponseHeader(in, out);
                } else if ((flag & 0xff) == FLAG_PING){
                    readPing(in, out);
                }
                break;
            case STATE_READ_HEADER:
                readRequestHeader(in, out);
                break;
            case STATE_READ_NAME:
                readName(in, out);
                break;
            case STATE_READ_PAYLOAD:
                readPayload(in, out);
                break;
            case STATE_READ_PAYLOAD_LENGTH:
                readResponsePayloadLength(in, out);
                break;
        }

    }

    private void readRequestHeader(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < numBytesToWait) {
            return;
        }

        int numOfParam = in.readByte();
        if (numOfParam == 3) {
            hasPayload = true;
        } else if (numOfParam == 2) {
            hasPayload = false;
        }
        in.skipBytes(13);
        int lengthOfName = in.readByte();
        state = STATE_READ_NAME;
        numBytesToWait = hasPayload ? lengthOfName + 4 : lengthOfName;
        readName(in, out);
    }

    private void readName(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < numBytesToWait) {
            return;
        }

        if (hasPayload) {
            in.skipBytes(numBytesToWait - 4);
            int lengthOfPayload = in.readInt();
            state = STATE_READ_PAYLOAD;
            numBytesToWait = lengthOfPayload;
            readPayload(in, out);
        } else {
            in.skipBytes(numBytesToWait);
            readFrame(in, out);
        }
    }

    private void readPayload(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < numBytesToWait) {
            return;
        }

        in.skipBytes(numBytesToWait);
        readFrame(in, out);
    }

    private void readFrame(ByteBuf in, List<Object> out) {

        int currentIndex = in.readerIndex();
        in.resetReaderIndex();
        int previousIndex = in.readerIndex();
        out.add(decode(in.readSlice(currentIndex - previousIndex)));
        state = STATE_INITIAL;
    }

    private NvaMessage decode(ByteBuf frame) {
        NvaMessage message = null;
        try {
            if ((flag & 0xff) == FLAG_REQUEST) {
                message = decodeRequest(frame);
            } else if ((flag & 0xff) == FLAG_RESPONSE) {
                message = decodeResponse(frame);
            } else if ((flag & 0xff) == FLAG_PING) {
                message = decodePing(frame);
            } else {
                throw new RuntimeException("fail to decode frame");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (message != null) {
            System.out.println("receive nva:" + message);
        }
        return message;
    }

    private NvaMessage decodeRequest(ByteBuf frame) throws Exception {
        //flag + num_of_param
        frame.skipBytes(2);
        int seq = frame.readInt();
        int version = frame.readByte();
        //length of command + command
        frame.skipBytes(8);
        int lengthOfName = frame.readByte();
        ByteBuf nameBytes = frame.readBytes(lengthOfName);
        String name = nameBytes.toString(StandardCharsets.UTF_8);
        Map<String, Object> payload = null;
        if (hasPayload) {
            int lengthOfPayload = frame.readInt();
            ByteBuf payloadBytes = frame.readBytes(lengthOfPayload);
            String payloadJson = payloadBytes.toString(StandardCharsets.UTF_8);
            payload = mapper.readValue(payloadJson, typeRef);
        }
        return new NvaRequest(seq, payload, version, "Command", name);
    }

    private NvaMessage decodeResponse(ByteBuf frame) throws Exception {
        //flag + num_of_param
        frame.skipBytes(2);
        int seq= frame.readInt();
        Map<String, Object> payload = null;
        if (hasPayload) {
            int lengthOfPayload = frame.readInt();
            ByteBuf payloadBytes = frame.readBytes(lengthOfPayload);
            String payloadJson = payloadBytes.toString(StandardCharsets.UTF_8);
            payload = mapper.readValue(payloadJson, typeRef);
        }
        return new NvaResponse(seq, payload);
    }

    private NvaMessage decodePing(ByteBuf frame) throws Exception {
        frame.skipBytes(2);
        int seq= frame.readInt();
        return new NvaPing(seq);
    }

    private void readResponseHeader(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < numBytesToWait) {
            return;
        }
        int numOfParam = in.readByte();
        hasPayload = numOfParam == 1;
        in.skipBytes(4);
        if (hasPayload) {
            state = STATE_READ_PAYLOAD_LENGTH;
            readResponsePayloadLength(in, out);
        } else {
            readFrame(in, out);
        }
    }

    private void readResponsePayloadLength(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }
        int length = in.readInt();
        state = STATE_READ_PAYLOAD;
        numBytesToWait = length;
        readPayload(in, out);
    }

    private void readPing(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }
        in.skipBytes(5);
        readFrame(in, out);
    }

}
