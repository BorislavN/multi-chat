package app.server.demo.endpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServerUtil {
    private long connectionId;
    private final int messageLimit;

    public ServerUtil(int messageLimit) {
        this.connectionId = 0;
        this.messageLimit = messageLimit;
    }

    public long getNextId() {
        return this.connectionId++;
    }

    public int getMessageLimit() {
        return this.messageLimit;
    }

    public boolean upgradeConnection(SocketChannel connection) {
        try {
            String request = ChannelHelper.readAllBytes(connection);
            request = request.trim();

            String response = FrameBuilder.buildUpgradeResponse(request);
            ChannelHelper.writeBytes(connection, ByteBuffer.wrap(response.getBytes(UTF_8)));

            if (response.startsWith("HTTP/1.1 400")) {
                return false;
            }

            return connection.isConnected();

        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Upgrade attempt failed - %s%n!", e.getMessage())
            );
        }
    }

    public FrameData readFrame(SocketChannel connection) {
        FrameData frameData = this.readMetadata(connection);
        String message = this.unmaskMessage(connection, frameData);

        frameData.setMessage(message);

        return frameData;
    }

    private FrameData readMetadata(SocketChannel connection) {
        try {
            ByteBuffer buffer = ChannelHelper.readBytes(connection, 2);

            boolean isFinished = getMostSignificantBit(buffer.get(0));
            int opcode = getOpcode(buffer.get(0));
            boolean isMasked = getMostSignificantBit(buffer.get(1));
            int length = getPayloadLength(buffer.get(1), connection);

            if (!isMasked) {
                throw new MalformedFrameException("Message not masked!");
            }

            buffer = ChannelHelper.readBytes(connection, 4);

            FrameData frameData = new FrameData(isFinished, opcode, isMasked, length);
            frameData.setMask(buffer.array());

            return frameData;

        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("An IOException occurred while reading frame metadata - %s!%n", e.getMessage())
            );
        }
    }

    private String unmaskMessage(SocketChannel connection, FrameData frameData) {
        try {
            ByteBuffer encodedBuffer = ChannelHelper.readBytes(connection, frameData.getLength());
            byte[] decoded = decodePayload(encodedBuffer.array(), frameData.getMask());

            String message = new String(decoded, UTF_8);

            if (frameData.getOpcode() == 8) {
                int statusCode = ((decoded[0] & 255) << 8) + (decoded[1] & 255);

                if (message.length() > 2) {
                    return String.format("%d, %s", statusCode, message.substring(2));
                }

                return String.valueOf(statusCode);
            }

            return message;

        } catch (IOException ex) {
            throw new IllegalStateException(
                    String.format("An IOException occurred while reading payload - %s%n!", ex.getMessage())
            );
        }
    }

    private boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }

    private int getOpcode(byte value) {
        return value & 15;
    }

    //16 bits => max unsigned value: 65535 bytes => 63KB
    //64 bits => max unsigned value: over 16 million TB :D
    //We need a message limit
    private int getPayloadLength(byte data, SocketChannel connection) throws IOException {
        int initialLength = data & 127;
        ByteBuffer extendedData;

        if (initialLength == 127) {
            extendedData = ChannelHelper.readBytes(connection, 8);

            long value = (Byte.toUnsignedLong(extendedData.get(0)) << 56)
                    + (Byte.toUnsignedLong(extendedData.get(1)) << 48)
                    + (Byte.toUnsignedLong(extendedData.get(2)) << 40)
                    + (Byte.toUnsignedLong(extendedData.get(3)) << 32)
                    + (Byte.toUnsignedLong(extendedData.get(4)) << 24)
                    + (Byte.toUnsignedLong(extendedData.get(5)) << 16)
                    + (Byte.toUnsignedLong(extendedData.get(6)) << 8)
                    + (Byte.toUnsignedLong(extendedData.get(7)));

            if (value > this.messageLimit) {
                throw new IllegalArgumentException(String.format("Message too long limit - %d!", this.messageLimit));
            }

            return (int) value;
        }

        if (initialLength == 126) {
            extendedData = ChannelHelper.readBytes(connection, 2);

            return (Byte.toUnsignedInt(extendedData.get(0)) << 8) + Byte.toUnsignedInt(extendedData.get(1));
        }

        return initialLength;
    }

    private byte[] decodePayload(byte[] encoded, byte[] mask) {
        byte[] decoded = new byte[encoded.length];

        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ mask[i % 4]);
        }

        return decoded;
    }
}