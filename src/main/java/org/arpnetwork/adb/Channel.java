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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class Channel {
    private static final int BUFFER_SIZE = 128 * 1024;

    private Connection mConn;
    private int mId;
    private int mRemoteId;

    private ByteBuf mBuf;

    private ChannelListener mListener;

    public interface ChannelListener {
        void onOpened(Channel ch);

        void onClosed(Channel ch);
    }

    public Channel(Connection conn, int id) {
        mConn = conn;
        mId = id;
        mRemoteId = -1;
        mBuf = Unpooled.buffer(BUFFER_SIZE);
    }

    public void setStreamListener(ChannelListener listener) {
        mListener = listener;
    }

    public void close() {
        mConn.close(mId, mRemoteId);
    }

    protected void write(byte[] data) {
        mConn.write(mId, mRemoteId, data);
    }

    public int id() {
        return mId;
    }

    public int remoteId() {
        return mRemoteId;
    }

    public boolean isOpened() {
        return mRemoteId > 0;
    }


    public void onOpened(int remoteId) {
        mRemoteId = remoteId;

        if (mListener != null) {
            mListener.onOpened(this);
        }
    }

    public void onClosed() {
        if (mListener != null) {
            mListener.onClosed(this);
        }
    }

    public void onData(byte[] data) throws IOException {
        mBuf.writeBytes(data);

        Object msg;
        while ((msg = decode(mBuf)) != null) {
            onRead(msg);
        }
    }

    protected abstract Object decode(ByteBuf buf);

    protected abstract void onRead(Object msg) throws IOException;
}
