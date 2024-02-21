package app.server.minimal.entity;


import app.server.minimal.exception.MalformedFrameException;
import app.server.minimal.exception.MessageLengthException;
import app.util.Constants;

import java.nio.ByteBuffer;

import static app.util.Constants.MESSAGE_LIMIT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FrameData {
    private int stage;
    private int attempts;
    private boolean readCompleted;
    private ByteBuffer metadata;
    private ByteBuffer extendedLength;
    private ByteBuffer mask;
    private ByteBuffer payload;

    public FrameData() {
        this.stage = 0;
        this.attempts = 0;
        this.readCompleted = false;
    }

    public int getStage() {
        return this.stage;
    }

    public void incrementStage() {
        this.stage++;

        if (this.stage == 4) {
            this.unmaskPayload();
            this.readCompleted = true;
        }
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
        if (this.metadata == null) {
            this.metadata = ByteBuffer.allocate(2);
        }

        return this.metadata;
    }

    public ByteBuffer getExtendedLength() {
        return this.extendedLength;
    }

    public void initExtendedLength(int length) {
        if (this.extendedLength == null) {
            this.extendedLength = ByteBuffer.allocate(length);
        }
    }

    public ByteBuffer getMask() {
        if (this.mask == null) {
            this.mask = ByteBuffer.allocate(4);
        }

        return this.mask;
    }


    public ByteBuffer getPayload() {
        return this.payload;
    }

    public void initPayload(int length) {
        if (this.payload == null) {
            this.payload = ByteBuffer.allocate(length);
        }
    }

    public boolean isReadCompleted() {
        return this.readCompleted;
    }

    public byte getFirstByte() {
        return this.metadata.get(0);
    }

    public byte getSecondByte() {
        return this.metadata.get(1);
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

    public int getPayloadLength() {
        if (this.extendedLength == null) {
            return this.parseInitialLength();
        }

        return this.parseExtendedLength();
    }

    @Override
    public String toString() {
        String delimiter = "///////////////////////";

        if (!this.readCompleted) {
            return "Frame not completed!";
        }

        return String.format("%s%nIs finished: %s%nOpcode: 0x%s%nIs masked: %s%nLength: %s%nMask: %s%nMessage: \"%s\"%n%s%n"
                , delimiter
                , this.isFinished()
                , Integer.toHexString(this.getOpcode())
                , this.isMasked()
                , this.getPayloadLength()
                , arrayAsBinaryString(this.mask)
                , this.getMessage()
                , delimiter
        );
    }

    public void validateMetadata() {
        if (!getMostSignificantBit(this.metadata.get(1))) {
            throw new MalformedFrameException("Frame is not masked!");
        }

        int reservedBitsValue = this.metadata.get(0) & 112;

        if (reservedBitsValue != 0) {
            throw new MalformedFrameException("Reserved bits set!");
        }

        int opcode = this.metadata.get(0) & 15;

        if (opcode == 0 || opcode == 1 || opcode == 8 || opcode == 9) {
            //Opcode is valid
            return;
        }

        throw new MalformedFrameException("Unsupported opcode!");
    }

    public int parseInitialLength() {
        return this.metadata.get(1) & 127;
    }

    public int parseExtendedLength() {
        if (this.extendedLength.limit() == 8) {
            long tempValue = (Byte.toUnsignedLong(this.extendedLength.get(0)) << 56)
                    + (Byte.toUnsignedLong(this.extendedLength.get(1)) << 48)
                    + (Byte.toUnsignedLong(this.extendedLength.get(2)) << 40)
                    + (Byte.toUnsignedLong(this.extendedLength.get(3)) << 32)
                    + (Byte.toUnsignedLong(this.extendedLength.get(4)) << 24)
                    + (Byte.toUnsignedLong(this.extendedLength.get(5)) << 16)
                    + (Byte.toUnsignedLong(this.extendedLength.get(6)) << 8)
                    + (Byte.toUnsignedLong(this.extendedLength.get(7)));



            if (tempValue > MESSAGE_LIMIT) {
                throw new MessageLengthException(String.format("Message too long limit - %d!", MESSAGE_LIMIT));
            }

            return (int) tempValue;
        }

        if (this.extendedLength.limit() == 2) {
            return (Byte.toUnsignedInt(this.extendedLength.get(0)) << 8) + Byte.toUnsignedInt(this.extendedLength.get(1));
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

    private void unmaskPayload() {
        for (int i = 0; i < this.payload.limit(); i++) {
            this.payload.put(i, (byte) (this.payload.get(i) ^ this.mask.get(i % 4)));
        }
    }

    private static boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }
}