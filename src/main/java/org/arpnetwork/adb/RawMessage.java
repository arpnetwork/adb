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

import io.netty.buffer.ByteBuf;

public class RawMessage {
    private byte[] mData;

    public RawMessage(String data) {
        this(data.getBytes());
    }

    public RawMessage(byte[] data) {
        mData = data;
    }

    public static RawMessage decodeFrom(ByteBuf buf) {
        RawMessage msg = null;

        int length = buf.readableBytes();
        if (length > 0) {
            msg = new RawMessage(buf.readBytes(length).array());
        }

        return msg;
    }

    public byte[] encodeToBytes() {
        return mData;
    }

    public byte[] data() {
        return mData;
    }

    @Override
    public String toString() {
        return String.format("[RAW, \"%s\"]", new String(mData).replaceAll("\n", "\\\\n"));
    }
}
