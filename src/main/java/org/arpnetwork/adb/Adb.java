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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

public class Adb {
    // Mode for normal file (664)
    public static final int MODE_NORMAL = 33204;
    // Mode for executable file (775)
    public static final int MODE_EXECUTABLE = 33277;

    private static final int DEFAULT_PORT = 5555;
    private static final int MAX_CHUNK_SIZE = 64 * 1024;

    private Connection mConn;

    /**
     * Connects to a device via TCP/IP.
     */
    public Adb(String host) throws IOException {
        this(host, DEFAULT_PORT);
    }

    /**
     * Connects to a device via TCP/IP.
     */
    public Adb(String host, int port) throws IOException {
        mConn = new Connection(host, port);
    }

    /**
     * Copies local file to device.
     */
    public void push(String localPath, String remotePath) throws IOException {
        push(localPath, remotePath, MODE_NORMAL);
    }

    /**
     * Copies local file to device with given mode.
     */
    public void push(String localPath, String remotePath, int mode) throws IOException {
        Stream s = mConn.open("sync:");

        String data = String.format(Locale.US, "%s,%d", remotePath, mode);
        s.write("SEND", data);

        FileInputStream fis = new FileInputStream(localPath);
        byte[] buf = new byte[MAX_CHUNK_SIZE];
        int bytes;
        while ((bytes = fis.read(buf)) != -1) {
            s.write("DATA", ByteBuffer.wrap(buf, 0, bytes));
        }
        fis.close();

        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        s.write("DONE", timestamp);
    }

    /**
     * Runs remote shell command.
     */
    public Stream shell(String cmd) throws IOException {
        return mConn.open("shell,v2,raw:" + cmd);
    }
}
