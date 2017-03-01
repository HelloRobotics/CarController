package io.github.hellorobotics.carcontroller.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Author: towdium
 * Date:   22/02/17.
 */

public class Instruction {
    public static final int LENGTH_DYNAMIC = -1;
    public static final byte ETB = 0x17;

    private enumInstruction code;
    private byte[] data;

    public Instruction(enumInstruction code, byte... data) throws IllegalArgumentException {
        this.code = code;
        this.data = data;
        if (code.getLen() != LENGTH_DYNAMIC) {
            if (data.length != code.getLen())
                throw new IllegalArgumentException("Instruction length mismatch");
        } else {
            if (code.isText() && (data.length != 19 && data[data.length - 1] != '\0'))
                throw new IllegalArgumentException("Illegal string message.");
        }
    }

    public Instruction(enumInstruction code, String data, boolean finished) throws IllegalArgumentException {
        if ((finished && data.length() > 18) || (!finished && data.length() > 19))
            throw new IllegalArgumentException("String size: " + data.length() +
                    ". Exceeds limitation for " + (finished ? "finished." : "unfinished."));
        this.code = code;
        this.data = new byte[finished ? data.length() + 1 : data.length()];
        byte[] buf = data.getBytes(Charset.forName("UTF-8"));
        System.arraycopy(buf, 0, this.data, 0, buf.length);
        if (finished) this.data[data.length()] = '\0';
    }

    public static Instruction fromByteArray(byte[] data) throws IllegalArgumentException {
        if (data.length > 20)
            throw new IllegalArgumentException(
                    "Data size: " + data.length + ". Exceeds limitation");
        ByteBuffer byteBuffer = ByteBuffer.allocate(20);
        byteBuffer.put(data).flip();
        byte buf[] = new byte[byteBuffer.remaining() - 2];
        byte b = byteBuffer.get();
        enumInstruction code = enumInstruction.fromByte(b);
        byteBuffer.get(buf, 0, byteBuffer.remaining() - 1);
        return new Instruction(code, buf);
    }

    public byte[] toByteArray() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(20);
        byteBuffer.put(code.toByte()).put(data).put(ETB);
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.position());
    }

    public enumInstruction getCode() {
        return code;
    }

    public byte getData(int index) {
        return data[index];
    }

    public byte[] getData() {
        return data;
    }

    public String getString() {
        return new String(data, 0, data.length - 1, Charset.forName("UTF-8"));
    }

    public boolean isFinished() {
        return !code.isText() || !(data.length == 19 && data[19] == '\0');
    }


    public enum enumInstruction {
        MOSI_TEXT, MOSI_REQUEST, MOSI_SPEED,
        MISO_TEXT, MISO_DISTANCE, MISO_DIRECTION;

        public static enumInstruction fromByte(byte b) {
            switch (b) {
                case 0x00:
                    return MISO_TEXT;
                case 0x01:
                    return MISO_DISTANCE;
                case 0x02:
                    return MISO_DIRECTION;
                default:
                    throw new UnsupportedOperationException("Code:" + String.format("0x%2x", b));
            }
        }

        public byte toByte() {
            switch (this) {
                case MOSI_TEXT:
                    return 0x00;
                case MOSI_REQUEST:
                    return 0x01;
                case MOSI_SPEED:
                    return 0x02;
                case MISO_TEXT:
                    return 0x00;
                case MISO_DISTANCE:
                    return 0x01;
                case MISO_DIRECTION:
                    return 0x02;
                default:
                    return (byte) 0xFF;
            }
        }

        public int getLen() {
            switch (this) {
                case MOSI_TEXT:
                    return LENGTH_DYNAMIC;
                case MOSI_REQUEST:
                    return LENGTH_DYNAMIC;
                case MOSI_SPEED:
                    return 2;
                case MISO_TEXT:
                    return LENGTH_DYNAMIC;
                case MISO_DISTANCE:
                    return 1;
                case MISO_DIRECTION:
                    return 1;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        public boolean isText() {
            return this == MISO_TEXT || this == MOSI_TEXT;
        }
    }
}
