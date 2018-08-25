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

public class RawChannel extends Channel {

    public interface RawListener {
        void onRaw(RawChannel ch, byte[] data);
    }

    private RawListener mListener;

    public RawChannel(Connection conn, int id) {
        super(conn, id);
    }

    public void setListener(RawListener listener) {
        mListener = listener;
    }

    public void write(String data) {
        RawMessage msg = new RawMessage(data);
        write(msg.encodeToBytes());
    }

    @Override
    protected Object decode(ByteBuf buf) {
        return RawMessage.decodeFrom(buf);
    }

    @Override
    protected void onRead(Object msg) throws IOException {
        if (mListener != null) {
            RawMessage m = (RawMessage) msg;
            mListener.onRaw(this, m.data());
        }
    }
}
