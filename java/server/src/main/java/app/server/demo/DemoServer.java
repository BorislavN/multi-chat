package app.server.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

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

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            //TODO: If we dont use iterator we get a false positive for OP_ACCEPT, but there is no connection to be accepted
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();


                try {
                    if (key.isValid() && key.isAcceptable()) {
                        SocketChannel connection = server.accept();

                        if (connection != null) {
                            Attachment attachment = new Attachment(util.getNextId());
                            connection.configureBlocking(false);

                            connection.register(selector, SelectionKey.OP_READ, attachment);

                            receivedConnection = true;
                        }
                    }

                    if (key.isValid() && key.isReadable()) {
                        SocketChannel connection = (SocketChannel) key.channel();
                        Attachment attachment = (Attachment) key.attachment();

                        if (attachment.wasUpgraded()) {
                            FrameData frameData = util.readMetadata(connection);
                            String message = util.unmaskMessage(connection, frameData.getLength(), frameData.getMask());

                            System.out.println(frameData);
                            System.out.println(message);
                            System.out.println();
                        }

                        if (!attachment.wasUpgraded()) {
                            boolean result = util.upgradeConnection(connection);

                            if (result) {
                                attachment.setWasUpgraded(true);
                            }
                        }
                    }

                } catch (MalformedFrameException | IllegalArgumentException | IllegalStateException e) {
                    System.err.println(e.getMessage());

                    key.channel().close();
                    selector.wakeup();
                }

                iterator.remove();
            }
        }

        //TODO: refactor old nio code, to close channels properly
        for (SelectionKey key : selector.keys()) {
            key.cancel();
            key.channel().close();
        }

        selector.close();
    }
}