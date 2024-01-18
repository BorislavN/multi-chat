package app.server.demo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

//TODO: check the NIO channel documentation for reading and writing, to avoid bugs
public class ServerUtil {
    //TODO: implement logic
    private long connectionId;
    private HashMap<String, StringBuilder> fragmentBuffer;

    public ServerUtil() {
        this.connectionId = 0;
        this.fragmentBuffer = new HashMap<>();
    }

    public long getNextId() {
        return this.connectionId++;
    }

    public boolean upgradeConnection(SocketChannel connection) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            StringBuilder message = new StringBuilder();

            int bytesRead = connection.read(buffer);

            this.checkForConnectionClose(bytesRead);

            do {
                message.append(UTF_8.decode(buffer.flip()));
                buffer.flip();
            } while (connection.read(buffer) > 0);

            String request = message.toString().trim();
            String response = FrameBuilder.buildUpgradeResponse(request);

            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes(UTF_8));

            while (responseBuffer.hasRemaining()) {
                connection.write(responseBuffer);
            }

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

    public FrameData readMetadata(SocketChannel connection) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            int bytesRead = connection.read(buffer);

            this.checkForConnectionClose(bytesRead);

            boolean isFinished = getMostSignificantBit(buffer.get(0));
            String opcode = getOpcode(buffer.get(0));
            boolean isMasked = getMostSignificantBit(buffer.get(1));
            int length = getPayloadLength(buffer.get(1), connection);

            if (!isMasked) {
                throw new MalformedFrameException("Message not masked!");
            }

            FrameData frameData = new FrameData(isFinished, opcode, isMasked, length);

            buffer = ByteBuffer.allocate(4);
            bytesRead = connection.read(buffer);

            this.checkForConnectionClose(bytesRead);

            frameData.setMask(buffer.array());

            return frameData;

        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("An IOException occurred while reading frame metadata - %s!%n", e.getMessage())
            );
        }
    }

    //TODO: implement logic fragmented messages
    // the java WebSocket Client has same sort of internal buffer, and sends the long messages as few fragmented ones - ignoring the finished flag
    // while the JS implementation sends the whole thing
    public String unmaskMessage(SocketChannel connection, int length, byte[] mask) {
        try {
            ByteBuffer encoded = ByteBuffer.allocate(length);
            int bytesRead = connection.read(encoded);

            this.checkForConnectionClose(bytesRead);

            return decodePayload(encoded.array(), mask);

        } catch (IOException ex) {
            throw new IllegalStateException(
                    String.format("An IOException occurred while reading payload - %s%n!", ex.getMessage())
            );
        }
    }

    private void checkForConnectionClose(int bytesRead) {
        if (bytesRead == -1) {
            throw new IllegalStateException("Connection was closed unexpectedly!");
        }
    }

    private boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }

    private String getOpcode(byte value) {
        return String.format("0x%s", Integer.toHexString(value & 15));
    }

    //16 bits => max unsigned value: 65535 bytes => 63KB
    //64 bits => max unsigned value: over 16 million TB :D
    //We need a message limit
    //I'm thinking of 100KB, that is an array with over 102,400 indexes
    private int getPayloadLength(byte data, SocketChannel connection) throws IOException {
        int initialLength = data & 127;
        ByteBuffer extendedData;

        if (initialLength == 127) {
            extendedData = ByteBuffer.allocate(8);
            int bytesRead = connection.read(extendedData);

            this.checkForConnectionClose(bytesRead);

            long value = (Byte.toUnsignedLong(extendedData.get(0)) << 56)
                    + (Byte.toUnsignedLong(extendedData.get(1)) << 48)
                    + (Byte.toUnsignedLong(extendedData.get(2)) << 40)
                    + (Byte.toUnsignedLong(extendedData.get(3)) << 32)
                    + (Byte.toUnsignedLong(extendedData.get(4)) << 24)
                    + (Byte.toUnsignedLong(extendedData.get(5)) << 16)
                    + (Byte.toUnsignedLong(extendedData.get(6)) << 8)
                    + (Byte.toUnsignedLong(extendedData.get(7)));

            //TODO: choose better limit
            if (value > 102400) {
                throw new IllegalArgumentException("Message too long - limit: 102400");
            }

            return (int) value;
        }

        if (initialLength == 126) {
            extendedData = ByteBuffer.allocate(2);
            int bytesRead = connection.read(extendedData);

            this.checkForConnectionClose(bytesRead);

            return (Byte.toUnsignedInt(extendedData.get(0)) << 8) + Byte.toUnsignedInt(extendedData.get(1));
        }

        return initialLength;
    }

    private String decodePayload(byte[] encoded, byte[] mask) {
        byte[] decoded = new byte[encoded.length];

        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ mask[i % 4]);
        }

        return new String(decoded, UTF_8);
    }
}