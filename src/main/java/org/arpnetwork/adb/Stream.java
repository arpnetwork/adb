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
import java.nio.ByteBuffer;

public class Stream {
    private static final byte STDIN = 0;
    private static final byte STDOUT = 1;
    private static final byte STDERR = 2;

    private Connection mConn;
    private int mId;
    private int mRemoteId;

    public Stream(Connection conn, int id, int remoteId) {
        mConn = conn;
        mId = id;
        mRemoteId = remoteId;
    }

    public void write(String id, String data) throws IOException {
        write(id, ByteBuffer.wrap(data.getBytes()));
    }

    public void write(String id, ByteBuffer data) throws IOException {
        ByteBuffer bb = AdbByteBuffer.allocate(id.length() + 4 + data.limit());
        bb.put(id.getBytes());
        AdbByteBuffer.putBytes(bb, data);
        mConn.write(mId, mRemoteId, bb.array());
    }

    public void write(String id, int value) throws IOException {
        ByteBuffer bb = AdbByteBuffer.allocate(id.length() + 4);
        bb.put(id.getBytes());
        bb.putInt(value);
        mConn.write(mId, mRemoteId, bb.array());
    }

    public void writeLine(String line) throws IOException {
        if (!line.endsWith("\n")) {
            line = line + "\n";
        }

        ByteBuffer bb = AdbByteBuffer.allocate(5 + line.length());
        bb.put(STDIN);
        AdbByteBuffer.putBytes(bb, line.getBytes());
        mConn.write(mId, mRemoteId, bb.array());
    }

    public String readLine() throws IOException {
        byte[] data = mConn.read(mId, mRemoteId);
        ByteBuffer bb = AdbByteBuffer.wrap(data);
        int id = bb.get();
        String line = new String(AdbByteBuffer.getBytes(bb)).trim();
        switch (id) {
            case STDOUT:
                return line;

            case STDERR:
                // FIXME
                throw new IOException("Unknown");

            default:
                throw new ProtocolException();
        }
    }
}
