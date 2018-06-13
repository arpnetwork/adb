package org.arpnetwork.adb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Util {
    public static String commandToString(int cmd) {
        return new String(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(cmd).array());
    }
}
