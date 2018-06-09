/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arpnetwork.adb;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class Message {
    private static final int HEADER_SIZE = 24;

    private int mCommand;
    private int mArg0;
    private int mArg1;
    private byte[] mData;

    public Message(int command, int arg0, int arg1) {
        this(command, arg0, arg1, new byte[0]);
    }

    public Message(int command, int arg0, int arg1, byte[] data) {
        mCommand = command;
        mArg0 = arg0;
        mArg1 = arg1;
        mData = data;
    }

    public static Message readFrom(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);

        // Header
        ByteBuffer bb = AdbByteBuffer.allocate(HEADER_SIZE);
        dis.readFully(bb.array());
        int command = bb.getInt();
        int arg0 = bb.getInt();
        int arg1 = bb.getInt();
        int length = bb.getInt();
        int crc32 = bb.getInt();
        int magic = bb.getInt();

        // Body
        byte[] data = new byte[length];
        dis.readFully(data);

        Message msg = new Message(command, arg0, arg1, data);
        if (msg.checksum() != crc32 || msg.magic() != magic) {
            throw new ProtocolException();
        }

        return msg;
    }

    public void writeTo(OutputStream os) throws IOException {
        ByteBuffer bb = AdbByteBuffer.allocate(HEADER_SIZE + mData.length);
        bb.putInt(mCommand);
        bb.putInt(mArg0);
        bb.putInt(mArg1);
        bb.putInt(mData.length);
        bb.putInt(checksum());
        bb.putInt(magic());
        bb.put(mData);
        os.write(bb.array());
    }

    public int command() {
        return mCommand;
    }

    public int arg0() {
        return mArg0;
    }

    public int arg1() {
        return mArg1;
    }

    public byte[] data() {
        return this.mData;
    }

    private int checksum() {
        int sum = 0;
        for (int i = 0; i < mData.length; ++i) {
            byte b = mData[i];
            sum += toUnsignedInt(b);
        }
        return sum;
    }

    private int magic() {
        return ~mCommand;
    }

    private static int toUnsignedInt(byte x) {
        return x & 0xFF;
    }
}
