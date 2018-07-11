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
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.ByteBuf;

public class SyncChannel extends Channel {
    // Mode for normal file (664)
    public static final int MODE_NORMAL = 33204;
    // Mode for executable file (775)
    public static final int MODE_EXECUTABLE = 33277;

    private LinkedBlockingQueue<SyncMessage> mMessages;

    public SyncChannel(Connection conn, int id) {
        super(conn, id);

        mMessages = new LinkedBlockingQueue<>();
    }

    public void send(String path, int mode) {
        write(new SyncMessage(SyncMessage.SEND, String.format("%s,%d", path, mode)));
    }

    public void recv(String path) {
        write(new SyncMessage(SyncMessage.RECV, path));
    }

    public void stat(String path) {
        write(new SyncMessage(SyncMessage.STA2, path));
    }

    public void writeData(String data) {
        writeData(data.getBytes());
    }

    public void writeData(byte[] data) {
        write(new SyncMessage(data));
    }

    public void writeData(byte[] data, int offset, int length) {
        write(new SyncMessage(data, offset, length));
    }

    public void writeDone(int timestamp) {
        write(new SyncMessage(timestamp));
    }

    public void syncWrite() throws IOException, InterruptedException {
        SyncMessage msg = mMessages.take();
        if (msg.id() != SyncMessage.OKAY) {
            throw new ProtocolException();
        }
    }

    public Stat syncStat() throws IOException, InterruptedException {
        SyncMessage msg = mMessages.take();
        if (msg.id() != SyncMessage.STA2) {
            throw new ProtocolException();
        }
        return new Stat(msg.data());
    }

    public byte[] readData() throws IOException, InterruptedException {
        SyncMessage msg = mMessages.take();
        switch (msg.id()) {
            case SyncMessage.DATA:
                return msg.data();

            case SyncMessage.DONE:
                return new byte[0];

            default:
                throw new ProtocolException();
        }
    }

    public void close() {
        write(new SyncMessage(SyncMessage.QUIT, ""));
    }

    @Override
    protected Object decode(ByteBuf buf) {
        return SyncMessage.decodeFrom(buf);
    }

    @Override
    protected void onRead(Object msg) {
        SyncMessage m = (SyncMessage) msg;

        try {
            mMessages.put(m);
        } catch (InterruptedException e) {
        }
    }

    private void write(SyncMessage msg) {
        write(msg.encodeToBytes());
    }
}
