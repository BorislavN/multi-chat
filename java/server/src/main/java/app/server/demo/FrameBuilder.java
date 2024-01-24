package app.server.demo;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FrameBuilder {
    public static String buildUpgradeResponse(String request) {
        boolean isGetRequest = request.startsWith("GET");

        String error ;

        if (!isGetRequest) {
            error = "Not a valid upgrade request, request must use HTTP \"GET\" method!";

            return createErrorResponse(error);
        }

        try {
            String websocketKey = validateHeaders(request);

            if (websocketKey != null) {
                String switchingProtocolsHeader = "HTTP/1.1 101 Switching Protocols";
                String connectionHeader = "Connection: Upgrade";
                String upgradeHeader = "Upgrade: websocket";

                String appendedKey = websocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                byte[] sha1Array = MessageDigest.getInstance("SHA-1").digest(appendedKey.getBytes(UTF_8));
                String websocketAcceptHeader = String.format("Sec-WebSocket-Accept: %s", Base64.getEncoder().encodeToString(sha1Array));

                return String.join(System.lineSeparator()
                        , switchingProtocolsHeader
                        , connectionHeader
                        , upgradeHeader
                        , websocketAcceptHeader
                        , System.lineSeparator());
            }

            error = "Verify that your request contains these headers: \"Upgrade: websocket\", \"Connection: Upgrade\", \"Sec-WebSocket-Key\", \"Sec-WebSocket-Version\"";

        } catch (NoSuchAlgorithmException e) {
            error = "Exception occurred while generating the \"Sec-WebSocket-Accept\", no such algorithm!";
        }

        return createErrorResponse(error);
    }


    public static String buildCloseFrame(String request) {


        return "";
    }

    public static String buildPingFrame(String request) {


        return "";
    }

    public static String buildPongFrame(String request) {


        return "";
    }

    public static ByteBuffer buildFrame(String message, boolean isFinished, int opcode) {
        ByteBuffer frame;

        byte firstByte = (byte) opcode;

        if (isFinished) {
            firstByte |= (byte) 0b10000000;
        }

        if (message.length() > 65535) {
            frame = ByteBuffer.allocate(10 + message.length());

            frame.put(0, firstByte);
            frame.put(1, (byte) 127);
            frame.put(2, splitLengthToBytes(8, message.length()));
            frame.put(3, message.getBytes(UTF_8));

            return frame;
        }

        if (message.length() > 125) {
            frame = ByteBuffer.allocate(4 + message.length());

            frame.put(0, firstByte);
            frame.put(1, (byte) 126);
            frame.put(2, splitLengthToBytes(2, message.length()));
            frame.put(3, message.getBytes(UTF_8));

            return frame;
        }

        if (message.isBlank()) {
            frame = ByteBuffer.allocate(4);

            frame.put(0, firstByte);
            frame.put(1, (byte) 2);
            frame.put(2, (byte) 0b011);
            frame.put(3, (byte) 0b11101000);

            return frame;
        }

        frame = ByteBuffer.allocate(2 + message.length());
        frame.put(0, firstByte);
        frame.put(1, (byte) message.length());
        frame.put(2, message.getBytes(UTF_8));

        return frame;
    }

    private static String validateHeaders(String request) {
        String[] headers = request.split("\r\n");

        boolean hasConnectionHeader = false;
        boolean hasUpgradeHeader = false;
        String websocketKey = null;
        boolean hasVersionHeader = false;

        for (String header : headers) {
            if ("Connection: Upgrade".equalsIgnoreCase(header)) {
                hasConnectionHeader = true;
            }

            if ("Upgrade: websocket".equalsIgnoreCase(header)) {
                hasUpgradeHeader = true;
            }

            if (header.startsWith("Sec-WebSocket-Key")) {
                websocketKey = header.substring("Sec-WebSocket-Key: ".length());
            }

            if ("Sec-WebSocket-Version: 13".equalsIgnoreCase(header)) {
                hasVersionHeader = true;
            }
        }

        if (hasVersionHeader && hasUpgradeHeader && hasConnectionHeader) {
            return websocketKey;
        }

        return null;
    }

    private static String createErrorResponse(String error) {
        String badRequestHeader = "HTTP/1.1 400 Bad Request";
        String supportedVersionHeader = "Sec-WebSocket-Version: 13";
        String contentTypeHeader = "Content-Type: text/plain";
        String contentLengthHeader = String.format("Content-Length: %d", error.getBytes(UTF_8).length);

        return String.join(System.lineSeparator()
                , badRequestHeader
                , supportedVersionHeader
                , contentTypeHeader
                , contentLengthHeader
                , ""
                , error);
    }

    private static byte[] splitLengthToBytes(int parts, int length) {
        byte[] bytes = new byte[parts];
        int step = 0;

        if (parts > 8) {
            throw new IllegalArgumentException("Part limit - 8 bytes! (64bit)");
        }

        for (int index = bytes.length - 1; index >= 0; index--, step += 8) {
            bytes[index] = (byte) (length >> step);
        }

        return bytes;
    }
}