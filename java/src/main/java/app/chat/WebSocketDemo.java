package app.chat;

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
public class WebSocketDemo {
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
            if (size == -1) return null;
            byte[] decoded = new byte[size - 6];
            byte[] key = new byte[]{data[2], data[3], data[4], data[5]};
            for (int i = 0; i < size - 6; i++) {
                decoded[i] = (byte) (data[i + 6] ^ key[i & 0x3]);
            }
            return new String(decoded, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "ping";
    }
}
