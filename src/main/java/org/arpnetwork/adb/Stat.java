package org.arpnetwork.adb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Stat {
    public long error;
    public long dev;
    public long ino;
    public long mode;
    public long nlink;
    public long uid;
    public long gid;
    public long size;
    public long atime;
    public long mtime;
    public long ctime;

    public Stat(byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        error = buf.readUnsignedIntLE();
        dev = buf.readLongLE();
        ino = buf.readLongLE();
        mode = buf.readUnsignedIntLE();
        nlink = buf.readUnsignedIntLE();
        uid = buf.readUnsignedIntLE();
        gid = buf.readUnsignedIntLE();
        size = buf.readLongLE();
        atime = buf.readLongLE();
        mtime = buf.readLongLE();
        ctime = buf.readLongLE();
    }
}
