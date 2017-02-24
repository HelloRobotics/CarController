package io.github.hellorobotics.carcontroller.utils;

/**
 * Author: towdium
 * Date:   24/02/17.
 */

public class Utilities {
    public static String arrayToString(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Data: ");
        for (byte b : data) {
            stringBuilder.append(String.format("0x%02x ", b));
        }
        return stringBuilder.toString();
    }

    public static int toUnsigned(byte b) {
        if ((int) b < 0) {
            return (int) b + 256;
        } else {
            return (int) b;
        }
    }
}
