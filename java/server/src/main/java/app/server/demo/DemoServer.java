package app.server.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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

//TODO: modify implementation to use ServerSocketChannel
public class DemoServer {

    public static void main(String[] args) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(80));
        server.configureBlocking(false);

        System.out.println("Server started on 127.0.0.1:80");

        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);

        boolean receivedConnection = false;
        ServerUtil util = new ServerUtil();

        while (selector.keys().size() > 1 || !receivedConnection) {
            selector.select();

            for (SelectionKey key : selector.selectedKeys()) {
                try {
                    if (key.isValid() && key.isAcceptable()) {
                        SocketChannel connection = server.accept();

                        if (connection != null) {
                            boolean result = util.upgradeConnection(connection);
                            receivedConnection = true;

                            if (result) {
                                connection.configureBlocking(false);
                                connection.register(selector, SelectionKey.OP_READ);
                            }
                        } else {

                            //TODO: why is the key invoked
                            System.out.println("Why are we here:D");
                        }
                    }

                    if (key.isValid() && key.isReadable()) {
                        SocketChannel connection = (SocketChannel) key.channel();

                        FrameData frameData = util.readMetadata(connection);
                        String message = util.unmaskMessage(connection, frameData.getLength(), frameData.getMask());

                        System.out.println(frameData);
                        System.out.println(message);
                        System.out.println();
                    }

                } catch (MalformedFrameException | IllegalArgumentException | IllegalStateException e) {

                    System.err.println(e.getMessage());
                    key.cancel();
                    key.channel().close();
                }
            }

        }

        //TODO: refactor old nio code, to close channels properly
        for (SelectionKey key : selector.keys()) {
            key.cancel();
            key.channel().close();
        }

        selector.close();
        server.close();
    }
}