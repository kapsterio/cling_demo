package com.test.server.nva;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 000000ED  e0 02 00 00 00 01 01 07  43 6f 6d 6d 61 6e 64 09   ........ Command.
 * 000000FD  47 65 74 56 6f 6c 75 6d  65                        GetVolum e
 *
 * 00000106  e0 03 00 00 00 02 01 07  43 6f 6d 6d 61 6e 64 04   ........ Command.
 * 00000116  50 6c 61 79 00 00 01 72  7b 22 61 75 74 6f 4e 65   Play...r {"autoNe
 * ...
 * 00000286  36 35 34 30 30 31 30 30  22 7d                     65400100 "}
 *
 *
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
    private final static int FLAG_REQUEST = 0xe0;
    private final static int FLAG_RESPONSE = 0xc0;
    private final static int FLAG_PING = 0xe4;

    private final static int STATE_INITIAL = 0;
    private final static int STATE_READ_HEADER = 1;
    private final static int STATE_READ_NAME = 2;
    private final static int STATE_READ_PAYLOAD = 3;
    private final static int STATE_READ_PAYLOAD_LENGTH = 4;

    private int state = STATE_INITIAL;
    private boolean hasPayload;
    private int numBytesToWait;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state) {
            case STATE_INITIAL:
                in.markReaderIndex();
                int flag = in.readByte();
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
        out.add(in.readRetainedSlice(currentIndex - previousIndex));
        state = STATE_INITIAL;
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
