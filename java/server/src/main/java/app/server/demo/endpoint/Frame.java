package app.server.demo.endpoint;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

//TODO: implement this class, refactor the code to use it instead of FrameData
// as by the RFC - in the absence of extensions, the implementations should not depend on specific frame boundaries
// currently my code depends on the clients sending the whole frame
// This is not a problem with the js implementation (it sends whole frames - both fragmented and not), but exceptions occur withe the java WebSocket
// the java implementation has an internal buffer of around 16000, and when splitting a large message,
// some payload bytes are cut, providing an incomplete frame
// this triggers exception in my server implementation, as it expects to get the payload in one go
// the problem can be masked by using a while loop when reading, it will wait the necessary time,
// but that opens a possibility that one slow connection will stall the whole server. Given the fact that im using a single thread and a selector,
// this class aims to  reduce latency
public class Frame {
    private int stage;
    private int attempts;
    private boolean readCompleted;
    private ByteBuffer metadata;
    private ByteBuffer extendedLength;
    private ByteBuffer mask;
    private ByteBuffer payload;

    public Frame() {
        this.stage = 0;
        this.attempts = 0;
        this.readCompleted = false;
    }

    public int getStage() {
        return this.stage;
    }

    public void incrementStep() {
        this.stage++;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public void incrementAttempts() {
        if (this.attempts == Constants.ATTEMPT_LIMIT) {
            throw new IllegalStateException("Frame read attempt limit reached!");
        }
        this.attempts++;
    }

    public ByteBuffer getMetadata() {
        return this.metadata;
    }

    public void setMetadata(ByteBuffer metadata) {
        this.metadata = metadata;
    }

    public ByteBuffer getExtendedLength() {
        return this.extendedLength;
    }

    public void setExtendedLength(ByteBuffer extendedLength) {
        this.extendedLength = extendedLength;
    }

    public ByteBuffer getMask() {
        return this.mask;
    }

    public void setMask(ByteBuffer mask) {
        this.mask = mask;
    }

    public ByteBuffer getPayload() {
        return this.payload;
    }

    public void setPayload(ByteBuffer payload) {
        this.payload = payload;
    }

    public boolean isReadCompleted() {
        return this.readCompleted;
    }

    public void setReadCompleted(boolean readCompleted) {
        this.readCompleted = readCompleted;
    }

    public boolean isFinished() {
        return getMostSignificantBit(this.metadata.get(0));
    }

    public int getOpcode() {
        return this.metadata.get(0) & 15;
    }

    public boolean isMasked() {
        return getMostSignificantBit(this.metadata.get(1));
    }

    public String getMessage() {
        String message = new String(this.payload.array(), UTF_8);

        if (this.getOpcode() == 8) {
            int statusCode = ((this.payload.get(0) & 255) << 8) + (this.payload.get(1) & 255);

            if (message.length() > 2) {
                return String.format("%d, %s", statusCode, message.substring(2));
            }

            return String.valueOf(statusCode);
        }

        return message;
    }

    public int getTotalLength() {
        return this.payload.limit();
    }

    @Override
    public String toString() {
        String delimiter = "///////////////////////";

        return String.format("%s%nIs finished: %s%nOpcode: 0x%s%nIs masked: %s%nLength: %s%nMask: %s%nMessage: \"%s\"%n%s%n"
                , delimiter
                , this.isFinished()
                , Integer.toHexString(this.getOpcode())
                , this.isMasked()
                , this.getTotalLength()
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


            if (tempValue > Constants.MESSAGE_LIMIT * 20) {
                throw new IllegalStateException(String.format("Message too long limit - %d!", Constants.MESSAGE_LIMIT));
            }

            return (int) tempValue;
        }

        if (data.limit() == 2) {
            return (Byte.toUnsignedInt(data.get(0)) << 8) + Byte.toUnsignedInt(data.get(1));
        }

        throw new MalformedFrameException("Unexpected value, extend length should be 2 or 8 bytes!");
    }

    private String arrayAsBinaryString(ByteBuffer mask) {
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < mask.limit(); index++) {
            output.append(mask.get(index) & 255);

            if (index < mask.limit() - 1) {
                output.append(" ");
            }
        }

        return output.toString();
    }

    private byte[] unmaskPayload() {
        byte[] decoded = new byte[payload.limit()];

        for (int i = 0; i < this.payload.limit(); i++) {
            decoded[i] = (byte) (this.payload.get(i) ^ this.mask.get(i % 4));
        }

        return decoded;
    }

    private static boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }
}