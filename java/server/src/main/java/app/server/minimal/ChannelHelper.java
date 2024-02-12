package app.server.minimal;


import app.util.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChannelHelper {

    public static String readAllBytes(SocketChannel connection) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        StringBuilder message = new StringBuilder();

        int bytesRead;

        while ((bytesRead = connection.read(buffer)) > 0) {
            message.append(UTF_8.decode(buffer.flip()));
            buffer.flip();
        }

        if (bytesRead == -1) {
            throw new IllegalStateException("Connection closed!");
        }

        if (message.isEmpty()) {
            throw new IllegalStateException(
                    String.format("No bytes could be read from connection - %s", connection.getRemoteAddress())
            );
        }

        return message.toString();
    }

    public static boolean readBytes(SocketChannel connection, ByteBuffer buffer) throws IOException {
        int bytesRead = connection.read(buffer);

        if (bytesRead == -1) {
            throw new IllegalStateException("Connection closed!");
        }

        return bytesRead == buffer.capacity();
    }

    public static void writeBytes(SocketChannel connection, ByteBuffer buffer) throws IOException {
        for (int attempts = 0; attempts < Constants.ATTEMPT_LIMIT; attempts++) {
            if (!buffer.hasRemaining()) {
                return;
            }

            int bytesWritten = connection.write(buffer);

            if (bytesWritten == -1) {
                throw new IllegalStateException("Connection closed!");
            }
        }

        if (buffer.hasRemaining()) {
            throw new IllegalStateException(
                    String.format("Write attempt limit reached - %s", connection.getRemoteAddress())
            );
        }
    }
}
