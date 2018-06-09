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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;

public class Connection {
    private static final int CNCX = 0x4e584e43;
    private static final int AUTH = 0x48545541;
    private static final int OPEN = 0x4e45504f;
    private static final int OKAY = 0x59414b4f;
    private static final int CLSE = 0x45534c43;
    private static final int WRTE = 0x45545257;

    private static final int VERSION = 0x01000000;
    private static final int MAXDATA = 1024 * 1024;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private int mSeq;

    public Connection(String host, int port) throws IOException {
        mSocket = new Socket(host, port);
        mInputStream = mSocket.getInputStream();
        mOutputStream = mSocket.getOutputStream();
        mSeq = 1;

        Message msg = send(new Message(CNCX, VERSION, MAXDATA));
        if (!(msg.command() == AUTH && msg.arg0() == AUTH_TOKEN && tryAuth(AUTH_SIGNATURE, msg.data()))) {
            throw new ProtocolException();
        }
    }

    public Stream open(String destination) throws IOException {
        Stream stream = null;

        Message msg = send(new Message(OPEN, mSeq++, 0, (destination + "\0").getBytes()));
        if (msg.command() == OKAY) {
            stream = new Stream(this, msg.arg1(), msg.arg0());
        }

        return stream;
    }

    public void write(int id, int remoteId, byte[] data) throws IOException {
        sendRequest(new Message(WRTE, id, remoteId, data));
    }

    public byte[] read(int id, int remoteId) throws IOException {
        Message msg = Message.readFrom(mInputStream);
        switch (msg.command()) {
            case CLSE:
                throw new EOFException();

            case WRTE:
                // FIXME: multi stream support
                if (id != msg.arg1() || remoteId != msg.arg0()) {
                    throw new ProtocolException();
                }

                sendReady(id, remoteId);
                return msg.data();

            default:
                throw new ProtocolException();
        }
    }

    private boolean tryAuth(int mode, byte[] token) throws IOException {
        byte[] data = null;
        switch (mode) {
            case AUTH_SIGNATURE:
                data = Auth.sign(token);
                break;

            case AUTH_RSAPUBLICKEY:
                data = Auth.publicKey();
                break;

            default:
                assert false;
        }

        Message msg = send(new Message(AUTH, mode, 0, data));
        switch (msg.command()) {
            case AUTH:
                return mode == AUTH_SIGNATURE ? tryAuth(AUTH_RSAPUBLICKEY, msg.data()) : false;

            case CNCX:
                return true;

            default:
                return false;
        }
    }

    private void sendRequest(Message msg) throws IOException {
        msg = send(msg);
        if (msg.command() != OKAY) {
            throw new ProtocolException();
        }
    }

    private void sendReady(int id, int remoteId) throws IOException {
        Message msg = new Message(OKAY, id, remoteId);
        msg.writeTo(mOutputStream);
    }

    private Message send(Message msg) throws IOException {
        msg.writeTo(mOutputStream);
        return Message.readFrom(mInputStream);
    }
}
