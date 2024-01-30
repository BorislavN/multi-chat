package app.server.demo.endpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServerUtil {
    private long connectionId;

    public ServerUtil() {
        this.connectionId = 0;
    }

    public long getNextId() {
        return this.connectionId++;
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
        try {
            ByteBuffer metadata = ChannelHelper.readBytes(connection, 2);

            FrameData.validateFirstByte(metadata.get(0));
            FrameData.validateSecondByte(metadata.get(1));
            int length = FrameData.parseInitialLength(metadata.get(1));
            ByteBuffer extendedLength = this.readExtendedLength(connection, length);

            if (extendedLength != null) {
                length = FrameData.parseExtendedLength(extendedLength);
            }

            ByteBuffer mask = ChannelHelper.readBytes(connection, 4);
            System.out.println("isFinished: "+((metadata.get(0)&128)==128));
            System.out.println("opcode: "+(metadata.get(0)&15));
            System.out.println("Need to read: "+length);
            ByteBuffer maskedPayload = ChannelHelper.readBytes(connection, length);

            return new FrameData(length,metadata,extendedLength,mask,maskedPayload);

        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("An IOException occurred while reading the frame - %s!%n", e.getMessage())
            );
        }
    }

    private ByteBuffer readExtendedLength(SocketChannel connection, int initialLength) throws IOException {
        if (initialLength == 127) {
            return ChannelHelper.readBytes(connection, 8);
        }

        if (initialLength == 126) {
            return ChannelHelper.readBytes(connection, 2);
        }

        return null;
    }
}