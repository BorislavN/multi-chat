package app.server.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        ServerSocket server = new ServerSocket(80);

        try {
            System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection…");
            Socket client = server.accept();
            System.out.println("A client connected.");

            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            Scanner s = new Scanner(in, "UTF-8");

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
                            + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                            + "\r\n\r\n").getBytes("UTF-8");
                    out.write(response, 0, response.length);

                    String message;

                    while ((message = decodeMessage(in)) != null && !message.isEmpty()) {
                        System.out.println(message);

                        if ("error".equals(message)) {
                            break;
                        }
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
            byte[] data = new byte[1024];
            int size = in.read(data);

            if (size == -1) {
                return null;
            }

            byte[] key = new byte[4];
            long finalLength = 0;
            long payloadStartIndex = -1;

            for (int index = 0; index < data.length; index++) {
                byte currentByte = data[index];

                String byteAsString = toBinaryString(currentByte);

                System.out.println();
                System.out.println("Byte number: " + (index + 1) + ", decimal - " + Byte.toUnsignedInt(currentByte) + ", binary - " + byteAsString);

                if (index == 0) {
                    char finished = byteAsString.charAt(0);
                    String opcode = byteAsString.substring(4);
                    System.out.println("Is finished: " + finished);
                    System.out.println("Opcode: 0x" + Integer.toHexString(Byte.parseByte(opcode, 2)));

                    continue;
                }

                if (index == 1) {
                    char masked = byteAsString.charAt(0);
                    byte length = Byte.parseByte(byteAsString.substring(1), 2);
                    finalLength = length;


                    if (length == 126) {
                        finalLength = Long.parseLong(toBinaryString(data[index + 1]) + toBinaryString(data[index + 2]), 2);

                        index += 2;
                    }

                    if (length == 127) {
                        finalLength = Long.parseLong(toBinaryString(data[index + 1]) + toBinaryString(data[index + 2])
                                + toBinaryString(data[index + 3]) + toBinaryString(data[index + 4])
                                + toBinaryString(data[index + 5]) + toBinaryString(data[index + 6])
                                + toBinaryString(data[index + 7]) + toBinaryString(data[index + 8]), 2);

                        index += 8;
                    }

                    System.out.println("Is masked: " + masked);
                    System.out.println("Length: " + finalLength);

                    continue;
                }

                key = new byte[]{currentByte, data[index + 1], data[index + 2], data[index + 3]};
                payloadStartIndex = index + 4;

                System.out.println("Mask: " + toBinaryString(key[0]) + " " + toBinaryString(key[1]) + " " + toBinaryString(key[2]) + " " + toBinaryString(key[3]));

                break;
            }

            System.out.println();

            byte[] decoded = new byte[(int) finalLength];

            for (int i = (int) payloadStartIndex; i < payloadStartIndex + finalLength; i++) {
                decoded[(int) (i - payloadStartIndex)] = (byte) (data[i] ^ key[(int) (i - payloadStartIndex) % 4]);
            }
            return new String(decoded, "UTF-8");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());

            return "error";
        }
    }

    private static String toBinaryString(byte currentByte) {
        return Integer.toBinaryString((currentByte & 255));
    }

}