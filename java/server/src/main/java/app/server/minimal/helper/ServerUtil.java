package app.server.minimal.helper;

import app.server.minimal.entity.ConnectionData;
import app.server.minimal.entity.FrameData;
import app.server.minimal.entity.UpgradeStatus;
import app.server.minimal.exception.ConnectionException;

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

    public UpgradeStatus upgradeConnection(SocketChannel connection) {
        try {
            String request = ChannelHelper.readAllBytes(connection);
            request = request.trim();

            String response = FrameBuilder.buildUpgradeResponse(request);
            ChannelHelper.writeBytes(connection, ByteBuffer.wrap(response.getBytes(UTF_8)));

            if (response.startsWith("HTTP/1.1 400")) {
                return new UpgradeStatus(false, true);
            }

            return new UpgradeStatus(connection.isConnected(), false);

        } catch (IOException e) {
            throw new ConnectionException(
                    String.format("Upgrade attempt failed - %s%n!", e.getMessage())
            );
        }
    }

    public void readFrame(ConnectionData connectionData) {
        try {
            SocketChannel connection = connectionData.getConnection();

            if (connectionData.getCurrentFrame().isReadCompleted()) {
                connectionData.resetCurrentFrame();
            }

            FrameData current = connectionData.getCurrentFrame();
            current.incrementAttempts();

            if (current.getStage() == 0) {
                if (!ChannelHelper.readBytes(connection, current.getMetadata())) {
                    return;
                }

                current.validateMetadata();

                current.incrementStage();
            }

            if (current.getStage() == 1) {
                this.readExtendedLength(connection, current);

                if (current.getExtendedLength() != null && current.getExtendedLength().hasRemaining()) {
                    return;
                }

                current.incrementStage();
            }

            if (current.getStage() == 2) {

                if (!ChannelHelper.readBytes(connection, current.getMask())) {
                    return;
                }

                current.incrementStage();
            }

            if (current.getStage() == 3) {
                current.initPayload(current.getPayloadLength());

                if (!ChannelHelper.readBytes(connection, current.getPayload())) {
                    return;
                }

                current.incrementStage();
            }

        } catch (IOException e) {
            throw new ConnectionException("Frame read operation failed!");
        }
    }

    private void readExtendedLength(SocketChannel connection, FrameData frameData) throws IOException {
        int initialLength = frameData.parseInitialLength();

        if (initialLength <= 125) {
            return;
        }

        if (initialLength == 127) {
            frameData.initExtendedLength(8);
        }

        if (initialLength == 126) {
            frameData.initExtendedLength(2);
        }

        ChannelHelper.readBytes(connection, frameData.getExtendedLength());
    }
}