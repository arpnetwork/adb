package org.arpnetwork.adb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ShellMessage {
    public static final int STDIN = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;
    public static final int EXIT = 3;

    private int mId;
    private String mData;

    public ShellMessage(int id, byte[] data) {
        this(id, new String(data));
    }

    public ShellMessage(int id, String data) {
        mId = id;
        mData = data;
    }

    public static ShellMessage decodeFrom(ByteBuf buf) {
        ShellMessage msg = null;

        buf.markReaderIndex();
        if (buf.readableBytes() >= 5) {
            int id = buf.readByte();
            int length = buf.readIntLE();
            if (buf.readableBytes() >= length) {
                msg = new ShellMessage(id, buf.readBytes(length).array());
            }
        }

        if (msg == null) {
            buf.resetReaderIndex();
        }

        return msg;
    }

    public byte[] encodeToBytes() {
        ByteBuf buf = Unpooled.buffer(5 + mData.length());
        buf.writeByte(mId);
        buf.writeIntLE(mData.length());
        buf.writeBytes(mData.getBytes());
        return buf.array();
    }

    public int id() {
        return mId;
    }

    public String data() {
        return mData;
    }

    public int code() {
        if (mId != EXIT) {
            throw new IllegalStateException();
        }

        return mData.charAt(0);
    }

    @Override
    public String toString() {
        String id = "UNKNOWN";
        switch (mId) {
            case STDIN:
                id = "STDIN";
                break;

            case STDOUT:
                id = "STDOUT";
                break;

            case STDERR:
                id = "STDERR";
                break;

            case EXIT:
                id = "EXIT";
                break;

            default:
                break;
        }

        if (mId != EXIT) {
            return String.format("[%s, \"%s\"]", id, mData.replaceAll("\n", "\\\\n"));
        } else {
            return String.format("[%s, %d]", id, code());
        }
    }
}
