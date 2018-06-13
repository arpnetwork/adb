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

import java.io.IOException;
import java.net.ProtocolException;

import io.netty.buffer.ByteBuf;

public class Message {
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

    public static Message readFrom(ByteBuf buf) throws IOException {
        // Header
        int command = buf.readIntLE();
        int arg0 = buf.readIntLE();
        int arg1 = buf.readIntLE();
        int length = buf.readIntLE();
        int crc32 = buf.readIntLE();
        int magic = buf.readIntLE();

        // Body
        ByteBuf body = buf.readBytes(length);

        Message msg = new Message(command, arg0, arg1, body.array());
        if (msg.checksum() != crc32 || msg.magic() != magic) {
            throw new ProtocolException();
        }

        return msg;
    }

    public void writeTo(ByteBuf buf) {
        buf.writeIntLE(mCommand);
        buf.writeIntLE(mArg0);
        buf.writeIntLE(mArg1);
        buf.writeIntLE(mData.length);
        buf.writeIntLE(checksum());
        buf.writeIntLE(magic());
        buf.writeBytes(mData);
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

    @Override
    public String toString() {
        return String.format("[%s, arg0=%d arg1=%d size=%d]", Util.commandToString(mCommand), mArg0, mArg1, mData.length);
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
