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

public class ShellChannel extends Channel {

    public interface ShellListener {
        void onStdout(ShellChannel ch, byte[] data);

        void onStderr(ShellChannel ch, byte[] data);

        void onExit(ShellChannel ch, int code);
    }

    private ShellListener mListener;

    public ShellChannel(Connection conn, int id) {
        super(conn, id);
    }

    public void setListener(ShellListener listener) {
        mListener = listener;
    }

    public void write(String data) {
        ShellMessage msg = new ShellMessage(ShellMessage.STDIN, data);
        write(msg.encodeToBytes());
    }

    @Override
    protected Object decode(ByteBuf buf) {
        return ShellMessage.decodeFrom(buf);
    }

    @Override
    protected void onRead(Object msg) throws IOException {
        if (mListener != null) {
            ShellMessage m = (ShellMessage) msg;
            switch (m.id()) {
                case ShellMessage.STDOUT:
                    mListener.onStdout(this, m.data());
                    break;

                case ShellMessage.STDERR:
                    mListener.onStderr(this, m.data());
                    break;

                case ShellMessage.EXIT:
                    mListener.onExit(this, m.code());
                    break;

                default:
                    throw new ProtocolException();
            }
        }
    }
}
