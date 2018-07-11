package org.arpnetwork.adb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SyncMessage {
    public static final int SEND = 0x444E4553;
    public static final int RECV = 0x56434552;
    public static final int DATA = 0x41544144;
    public static final int DONE = 0x454E4F44;
    public static final int OKAY = 0x59414B4F;
    public static final int QUIT = 0x54495551;
    public static final int STA2 = 0x32415453;

    private static final int STA2_SIZE = 68;

    private int mId;
    private int mTimestamp;
    private byte[] mData;
    private int mOffset;
    private int mLength;

    public SyncMessage(int id, String data) {
        this(id, data.getBytes());
    }

    public SyncMessage(int id, byte[] data) {
        this(id, data, 0, data.length);
    }

    public SyncMessage(int id, byte[] data, int offset, int length) {
        mId = id;
        mData = data;
        mOffset = offset;
        mLength = length;
    }

    public SyncMessage(byte[] data) {
        this(DATA, data);
    }

    public SyncMessage(byte[] data, int offset, int length) {
        this(DATA, data, offset, length);
    }

    public SyncMessage(int timestamp) {
        this(DONE, new byte[0]);
        mTimestamp = timestamp;
    }

    public static SyncMessage decodeFrom(ByteBuf buf) {
        SyncMessage msg = null;

        buf.markReaderIndex();
        if (buf.readableBytes() >= 8) {
            int id = buf.readIntLE();
            int length;
            if (id == STA2) {
                length = STA2_SIZE;
            } else {
                length = buf.readIntLE();
            }
            if (buf.readableBytes() >= length) {
                msg = new SyncMessage(id, buf.readBytes(length).array());
            }
        }

        if (msg == null) {
            buf.resetReaderIndex();
        }

        return msg;
    }

    public byte[] encodeToBytes() {
        ByteBuf buf = Unpooled.buffer(8 + mLength);
        buf.writeIntLE(mId);
        buf.writeIntLE(mId == DONE ? mTimestamp : mLength);
        if (mLength > 0) {
            buf.writeBytes(mData, mOffset, mLength);
        }
        return buf.array();
    }

    public int id() {
        return mId;
    }

    public byte[] data() {
        return mData;
    }

    @Override
    public String toString() {
        String id = new String(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mId).array());

        return String.format("[%s, %d]", id, mData.length);
    }
}
