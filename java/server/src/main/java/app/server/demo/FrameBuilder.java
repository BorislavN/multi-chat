package app.server.demo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FrameBuilder {
    public static String buildUpgradeResponse(String request) {
        String badRequestHeader = "HTTP/1.1 400 Bad Request";
        String supportedVersionHeader = "Sec-WebSocket-Version: 13";
        String contentTypeHeader = "Content-Type: text/plain";

        boolean isGetRequest = request.startsWith("GET");

        String error = "";

        if (!isGetRequest) {
            error = "Not a valid upgrade request, request must use HTTP \"GET\" method!";
        }

        //TODO: refactor, find why server is blocking.... :D
        if (error.isBlank()) {
            try {
                Pattern regex = Pattern.compile("Connection: (?<connection>.*)|Upgrade: (?<upgrade>.*)|Sec-WebSocket-Key: (?<key>.*)|Sec-WebSocket-Version: (?<version>\\d+)");
                Matcher matcher = regex.matcher(request);

                String connectionHeader =null;
                String upgradeHeader = null;
                String keyHeader = null;
                String versionHeader = null;

                while (matcher.find()){
                    connectionHeader = matcher.group("connection")!=null? matcher.group("connection") : connectionHeader;
                    upgradeHeader = matcher.group("upgrade")!=null? matcher.group("upgrade") : upgradeHeader;
                    keyHeader = matcher.group("key")!=null? matcher.group("key") : keyHeader;
                    versionHeader = matcher.group("version")!=null? matcher.group("version") : versionHeader;
                }

                if (validateMatches(connectionHeader, upgradeHeader, keyHeader, versionHeader)) {
                    String switchingProtocolsHeader = "HTTP/1.1 101 Switching Protocols";
                    connectionHeader = "Connection: Upgrade";
                    upgradeHeader = "Upgrade: websocket";

                    String appendedKey = keyHeader + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                    byte[] sha1Array = MessageDigest.getInstance("SHA-1").digest(appendedKey.getBytes(UTF_8));
                    String websocketAcceptHeader = String.format("Sec-WebSocket-Accept: %s", Base64.getEncoder().encodeToString(sha1Array));

                    return String.join(System.lineSeparator()
                            , switchingProtocolsHeader
                            , connectionHeader
                            , upgradeHeader
                            , websocketAcceptHeader
                            , "");
                }

                error = "Verify that your request contains these headers: \"Upgrade: websocket\", \"Connection: Upgrade\", \"Sec-WebSocket-Key\", \"Sec-WebSocket-Version\"";

            } catch (NoSuchAlgorithmException e) {
                error = "Exception occurred while generating the \"Sec-WebSocket-Accept\", no such algorithm!";
            }
        }

        String contentLengthHeader = String.format("Content-Length: %d", error.getBytes(UTF_8).length);

        return String.join(System.lineSeparator()
                , badRequestHeader
                , supportedVersionHeader
                , contentTypeHeader
                , contentLengthHeader
                , ""
                , error);
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

    public static String buildMessageFrame(String request) {


        return "";
    }

    private static boolean validateMatches(String connection, String upgrade, String key, String version) {
        if (!"Upgrade".equalsIgnoreCase(connection)) {
            return false;
        }

        if (!"websocket".equalsIgnoreCase(upgrade)) {
            return false;
        }

        if (key == null || key.isBlank()) {
            return false;
        }

        if (!"13".equals(version)) {
            return false;
        }

        return true;
    }
}