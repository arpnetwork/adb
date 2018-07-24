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
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;

public class Connection implements NettyConnection.ConnectionListener {
    private static final int CNCX = 0x4e584e43;
    private static final int AUTH = 0x48545541;
    private static final int OPEN = 0x4e45504f;
    private static final int OKAY = 0x59414b4f;
    private static final int CLSE = 0x45534c43;
    private static final int WRTE = 0x45545257;

    private static final int VERSION = 0x01000000;
    private static final int MAXDATA = 1024 * 1024;
    private static final int MAXDATA_OLD = 1024 * 4;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private Auth mAuth;
    private String mHost;
    private int mPort;

    private NettyConnection mConn;
    private State mState;
    private ConcurrentHashMap<Integer, Channel> mChannels;
    private int mSeq;
    private ConnectionListener mListener;

    public interface ConnectionListener {
        void onConnected(Connection conn);

        void onClosed(Connection conn);

        void onAuth(Connection conn, String key);

        void onException(Connection conn, Throwable cause);
    }

    private enum State {
        IDLE,
        CONNECTING,
        AUTH_SIGNATURE,
        AUTH_RSAPUBLICKEY,
        CONNECTED,
    }

    public Connection(Auth auth, String host, int port) {
        mAuth = auth;
        mHost = host;
        mPort = port;
        mState = State.IDLE;
        mChannels = new ConcurrentHashMap<>();
        mSeq = 1;
    }

    public synchronized void setListener(ConnectionListener listener) {
        mListener = listener;
    }

    public synchronized void connect() {
        assertState(State.IDLE);

        mConn = new NettyConnection(mHost, mPort);
        mConn.setListener(this);
        mState = State.CONNECTING;
        mConn.connect();
    }

    public synchronized void auth() {
        assertState(State.AUTH_SIGNATURE);

        mState = State.AUTH_RSAPUBLICKEY;
        byte[] key = (mAuth.getPublicKey() + " ARP\0").getBytes();
        mConn.write(new Message(AUTH, AUTH_RSAPUBLICKEY, 0, key));
    }

    public synchronized void close() {
        mConn.setListener(null);
        mConn.close();
        mConn = null;
        reset();
    }

    public synchronized ShellChannel openShell(String cmd) {
        int id = open("shell,v2,raw:" + cmd);

        ShellChannel ss = new ShellChannel(this, id);
        mChannels.put(id, ss);
        return ss;
    }

    public synchronized SyncChannel openSync() {
        int id = open("sync:");

        SyncChannel ss = new SyncChannel(this, id);
        mChannels.put(id, ss);
        return ss;
    }

    public synchronized void write(int id, int remoteId, ByteBuf buf) {
        write(id, remoteId, buf.array());
    }

    public synchronized void write(int id, int remoteId, byte[] data) {
        if (mState != State.CONNECTED) {
            throw new IllegalStateException();
        }

        mConn.write(new Message(WRTE, id, remoteId, data));
    }

    public synchronized void close(int id, int remoteId) {
        mConn.write(new Message(CLSE, id, remoteId));
    }

    @Override
    public void onConnected(NettyConnection conn) {
        conn.write(new Message(CNCX, VERSION, MAXDATA));
    }

    @Override
    public synchronized void onClosed(NettyConnection conn) {
        reset();

        if (mListener != null) {
            mListener.onClosed(this);
        }
    }

    @Override
    public synchronized void onMessage(NettyConnection conn, Message msg) throws Exception {
        switch (msg.command()) {
            case AUTH:
                assertProtocol(msg.arg0() == AUTH_TOKEN);
                onAuth(msg.data());
                break;

            case CNCX:
                if (msg.arg1() == MAXDATA_OLD) {
                    throw new ProtocolException("Unsupported ADB Protocol Version.");
                }

                onConnected();
                break;

            case OKAY:
                onChannelOkay(msg.arg1(), msg.arg0());
                break;

            case WRTE:
                onChannelData(msg.arg1(), msg.arg0(), msg.data());
                break;

            case CLSE:
                onChannelClosed(msg.arg1(), msg.arg0());
                break;

            default:
                assertProtocol(false);
                break;
        }
    }

    @Override
    public synchronized void onException(NettyConnection conn, Throwable cause) {
        reset();

        if (mListener != null) {
            mListener.onException(this, cause);
        }
    }

    private void onAuth(byte[] token) throws IOException {
        switch (mState) {
            case CONNECTING:
                mState = State.AUTH_SIGNATURE;
                mConn.write(new Message(AUTH, AUTH_SIGNATURE, 0, mAuth.sign(token)));
                break;

            case AUTH_SIGNATURE:
                if (mListener != null) {
                    mListener.onAuth(this, mAuth.getPublicKeyDigest());
                }
                break;

            default:
                assertProtocol(false);
        }
    }

    private void onConnected() {
        mState = State.CONNECTED;

        if (mListener != null) {
            mListener.onConnected(this);
        }
    }

    private void onChannelOkay(int id, int remoteId) throws IOException {
        Channel s = mChannels.get(id);
        assertProtocol(s != null);
        if (s.remoteId() == -1) {
            s.onOpened(remoteId);
        }
    }

    private void onChannelClosed(int id, int remoteId) throws IOException {
        Channel s = mChannels.get(id);
        if (s != null) {
            assertProtocol(s.remoteId() == remoteId);
            mChannels.remove(id);
            s.onClosed();
        }
    }

    private void onChannelData(int id, int remoteId, byte[] data) throws IOException {
        Channel s = mChannels.get(id);
        assertProtocol(s != null && s.remoteId() == remoteId);
        sendReady(id, remoteId);
        s.onData(data);
    }

    private int open(String destination) {
        assertState(State.CONNECTED);

        int id = mSeq++;
        mConn.write(new Message(OPEN, id, 0, (destination + "\0").getBytes()));
        return id;
    }

    private void reset() {
        mState = State.IDLE;

        for (Channel ch : mChannels.values()) {
            if (ch.isOpened()) {
                ch.onClosed();
            }
        }
        mChannels.clear();
    }

    private void sendReady(int id, int remoteId) {
        mConn.write(new Message(OKAY, id, remoteId));
    }

    private void assertProtocol(boolean expression) throws ProtocolException {
        if (!expression) {
            throw new ProtocolException("Invalid ADB Protocol");
        }
    }

    private void assertState(State state) {
        if (mState != state) {
            throw new IllegalStateException();
        }
    }
}
