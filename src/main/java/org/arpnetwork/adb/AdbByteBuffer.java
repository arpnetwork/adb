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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class AdbByteBuffer {
    /**
     * Allocates a new byte buffer with little endian byte order.
     */
    public static ByteBuffer allocate(int capacity) {
        return toLE(ByteBuffer.allocate(capacity));
    }

    /**
     * Wraps a byte array into a buffer with little endian byte order.
     */
    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * Wraps a byte array into a buffer with little endian byte order.
     */
    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        return toLE(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Modifies buffer's byte order to little endian.
     */
    public static ByteBuffer toLE(ByteBuffer bb) {
        return bb.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Transfers the bytes remaining in the given source
     * buffer into buffer, with leading size.
     */
    public static ByteBuffer putBytes(ByteBuffer bb, ByteBuffer value) {
        return bb.putInt(value.limit()).put(value);
    }

    /**
     * Transfers the entire content of the given source
     * byte array into buffer, with leading size.
     */
    public static ByteBuffer putBytes(ByteBuffer bb, byte[] value) {
        return bb.putInt(value.length).put(value);
    }

    /**
     * Reads the next sized bytes at the buffer's current position.
     */
    public static byte[] getBytes(ByteBuffer bb) {
        int length = bb.getInt();
        byte[] value = new byte[length];
        bb.get(value);

        return value;
    }
}
