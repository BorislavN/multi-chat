package app.server.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*JS code required:
const socket = new WebSocket("ws://localhost:80");

socket.addEventListener("open", (event) => {
  socket.send("Hello Server! This is JS!");

  socket.send("Sample message 1");
  socket.send("Sample message 2");
  socket.send("Sample message 3");

  setTimeout(()=>socket.close(), 1500);
});
*/

//Sample from: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java
public class DemoServer {
    private static final String DELIMITER = "///////////////////////";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        ServerSocket server = new ServerSocket(80);

        try {
            System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection…");
            Socket client = server.accept();
            System.out.println("A client connected.");

            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            Scanner s = new Scanner(in, StandardCharsets.UTF_8);

            try {
                String data = s.useDelimiter("\\r\\n\\r\\n").next();
                Matcher get = Pattern.compile("^GET").matcher(data);

                if (get.find()) {
                    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                    match.find();
                    byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                            + "Connection: Upgrade\r\n"
                            + "Upgrade: websocket\r\n"
                            + "Sec-WebSocket-Accept: "
                            + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                            + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                    out.write(response, 0, response.length);

                    String message;

                    while (!"error".equals(message = decodeMessage(in))) {
                        System.out.printf("Decoded message: \"%s\"%n", message);
                        System.out.println(DELIMITER);
                    }
                }
            } finally {
                s.close();
            }
        } finally {
            server.close();
        }
    }

    //Taken from: https://stackoverflow.com/questions/8870833/decode-send-message-from-websocket-with-java
    private static String decodeMessage(InputStream in) {
        try {
            byte[] data = new byte[2];
            int size = in.read(data);

            if (size == -1) {
                System.err.println("End of stream!");
                return "error";
            }

//            long finalLength = 0;

            boolean isFinished = getMostSignificantBit(data[0]);
            String opcode = getOpcode(data[0]);

            boolean isMasked = getMostSignificantBit(data[1]);
            int length = data[1] & 127;

            System.out.println();
            System.out.println(DELIMITER);
            System.out.printf("Is finished: %s%nOpcode: %s%n%n", isFinished, opcode);
            System.out.printf("Is masked: %s%nLength: %s%n%n", isMasked, length);

            if (!isMasked) {
                System.err.println("Message not masked!");
                return "error";
            }

            if (length > 125) {
                System.err.println("Length not yet supported!");
                return "error";
            }

            if (length < 1) {
                System.err.println("No Payload!");
                return "error";
            }

            byte[] mask = new byte[4];
            size = in.read(mask);

            if (size == -1) {
                System.err.println("Mask could not be read");
                return "error";
            }

            System.out.printf("Mask: %s%n%n", joinArrayToBinaryString(mask, " "));

//                TODO: implement logic for bigger payloads and fragmented messages
//                if (index == 1) {
//                    char masked = byteAsString.charAt(0);
//                    byte length = Byte.parseByte(byteAsString.substring(1), 2);
//                    finalLength = length;
//
//
//                    if (length == 126) {
//                        finalLength = Long.parseLong(toBinaryString(data[index + 1]) + toBinaryString(data[index + 2]), 2);
//
//                        index += 2;
//                    }
//
//                    if (length == 127) {
//                        finalLength = Long.parseLong(toBinaryString(data[index + 1]) + toBinaryString(data[index + 2])
//                                + toBinaryString(data[index + 3]) + toBinaryString(data[index + 4])
//                                + toBinaryString(data[index + 5]) + toBinaryString(data[index + 6])
//                                + toBinaryString(data[index + 7]) + toBinaryString(data[index + 8]), 2);
//
//                        index += 8;
//                    }
//
//                    System.out.println("Is masked: " + masked);
//                    System.out.println("Length: " + finalLength);
//
//                    continue;
//                }

            byte[] encoded = new byte[length];
            size = in.read(encoded);

            if (size == -1) {
                System.err.println("Payload could not be read!");
                return "error";
            }

            return unmaskMessage(encoded, mask);

        } catch (IOException ex) {
            System.err.println(ex.getMessage());

            return "error";
        }
    }

    private static boolean getMostSignificantBit(byte value) {
        return (value & 128) == 128;
    }

    private static String getOpcode(byte value) {
        return String.format("0x%s", Integer.toHexString(value & 15));
    }

    private static String joinArrayToBinaryString(byte[] data, String delimiter) {
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < data.length; index++) {
            output.append(data[index] & 255);

            if (index < data.length - 1) {
                output.append(delimiter);
            }
        }

        return output.toString();
    }

    private static String unmaskMessage(byte[] encoded, byte[] mask) {
        byte[] decoded = new byte[encoded.length];

        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ mask[i % 4]);
        }

        return new String(decoded, StandardCharsets.UTF_8);
    }
}