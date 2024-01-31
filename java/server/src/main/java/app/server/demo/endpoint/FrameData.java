package app.server.demo.endpoint;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FrameData {
    private final byte firstByte;
    private final byte secondByte;
    private byte[] extendedLength;
    private final byte[] mask;
    private byte[] payload;
    private final int totalLength;

    public FrameData(int totalLength, ByteBuffer metadata, ByteBuffer extendedLength, ByteBuffer mask, ByteBuffer maskedPayload) {
        this.totalLength = totalLength;
        this.firstByte = metadata.get(0);
        this.secondByte = metadata.get(1);
        this.setExtendedLength(extendedLength);
        this.mask = mask.array();
        this.setPayload(maskedPayload.array());
    }

    private void setExtendedLength(ByteBuffer extendedLength) {
        if (extendedLength == null) {
            this.extendedLength = null;

            return;
        }

        this.extendedLength = extendedLength.array();
    }

    private void setPayload(byte[] maskedPayload) {
        this.payload = this.unmaskPayload(maskedPayload);
    }

    public byte getFirstByte() {
        return this.firstByte;
    }

    public byte getSecondByte() {
        return this.secondByte;
    }

    public byte[] getExtendedLength() {
        return this.extendedLength;
    }

    public byte[] getMask() {
        return this.mask;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public boolean isFinished() {
        return getMostSignificantBit(this.firstByte);
    }

    public int getOpcode() {
        return this.firstByte & 15;
    }

    public boolean isMasked() {
        return getMostSignificantBit(this.secondByte);
    }

    public String getMessage() {
        String message = new String(this.payload, UTF_8);

        if (this.getOpcode() == 8) {
            int statusCode = ((this.payload[0] & 255) << 8) + (this.payload[1] & 255);

            if (message.length() > 2) {
                return String.format("%d, %s", statusCode, message.substring(2));
            }

            return String.valueOf(statusCode);
        }

        return message;
    }

    public int getTotalLength() {
        return this.totalLength;
    }

    @Override
    public String toString() {
        String delimiter = "///////////////////////";

        return String.format("%s%nIs finished: %s%nOpcode: 0x%s%nIs masked: %s%nLength: %s%nMask: %s%nMessage: \"%s\"%n%s%n"
                , delimiter
                , this.isFinished()
                , Integer.toHexString(this.getOpcode())
                , this.isMasked()
                , this.totalLength
                , arrayAsBinaryString(this.mask)
                , this.getMessage()
                , delimiter
        );
    }

    public static void validateFirstByte(byte firstByte) {
        int reservedBitsValue = firstByte & 112;

        if (reservedBitsValue != 0) {
            throw new MalformedFrameException("Reserved bits set!");
        }

        int opcode = firstByte & 15;

        if (opcode == 0 || opcode == 1 || opcode == 8 || opcode == 9) {
            //Opcode is valid
            return;
        }

        throw new MalformedFrameException("Unsupported opcode!");
    }

    public static void validateSecondByte(byte secondByte) {
        if (!getMostSignificantBit(secondByte)) {
            throw new MalformedFrameException("Frame is not masked!");
        }
    }

    public static int parseInitialLength(byte secondByte) {
        return secondByte & 127;
    }

    public static int parseExtendedLength(ByteBuffer data) {
        if (data.limit() == 8) {
            long tempValue = (Byte.toUnsignedLong(data.get(0)) << 56)
                    + (Byte.toUnsignedLong(data.get(1)) << 48)
                    + (Byte.toUnsignedLong(data.get(2)) << 40)
                    + (Byte.toUnsignedLong(data.get(3)) << 32)
                    + (Byte.toUnsignedLong(data.get(4)) << 24)
                    + (Byte.toUnsignedLong(data.get(5)) << 16)
                    + (Byte.toUnsignedLong(data.get(6)) << 8)
                    + (Byte.toUnsignedLong(data.get(7)));


            if (tempValue > Constants.MESSAGE_LIMIT *20) {
                throw new IllegalStateException(String.format("Message too long limit - %d!", Constants.MESSAGE_LIMIT));
            }

            return (int) tempValue;
        }

        if (data.limit() == 2) {
            return (Byte.toUnsignedInt(data.get(0)) << 8) + Byte.toUnsignedInt(data.get(1));
        }

        throw new MalformedFrameException("Unexpected value, extend length should be 2 or 8 bytes!");
    }

    private String arrayAsBinaryString(byte[] data) {
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < data.length; index++) {
            output.append(data[index] & 255);

            if (index < data.length - 1) {
                output.append(" ");
            }
        }

        return output.toString();
    }

    private byte[] unmaskPayload(byte[] maskedPayload) {
        byte[] decoded = new byte[maskedPayload.length];

        for (int i = 0; i < maskedPayload.length; i++) {
            decoded[i] = (byte) (maskedPayload[i] ^ this.mask[i % 4]);
        }

        return decoded;
    }

    private static boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }
}