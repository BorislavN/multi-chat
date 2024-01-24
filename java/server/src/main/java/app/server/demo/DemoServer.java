package app.server.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*JS code required:
const socket = new WebSocket("ws://localhost:80");

socket.addEventListener("open", (event) => {
  socket.send("Hello Server! This is JS!");

  socket.send("Sample message 1");
  socket.send("Sample message 2");
  socket.send("Sample message 3");

  setTimeout(()=>socket.close(1000), 1500);
});
*/

//TODO: read RFC, implement fragmentation support, ping, pong, close requests, send function
public class DemoServer {
    private ServerSocketChannel server;
    private Selector selector;
    private final ServerUtil utilities;
    private final Map<Long, ConnectionData> activeConnections;
    private boolean receivedConnection;

    public DemoServer(int port) {
        this.activeConnections = new HashMap<>();
        this.receivedConnection = false;
        this.utilities = new ServerUtil();

        try {
            this.server = ServerSocketChannel.open();
            this.server.bind(new InetSocketAddress(port));
            this.server.configureBlocking(false);

            System.out.println("Server starting on 127.0.0.1:80...");

            this.selector = Selector.open();
            this.server.register(this.selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            Logger.logError("Server failed to start up", e);
        }
    }

    public void start() {
        if (this.isRunning()) {
            try {
                while (!this.activeConnections.isEmpty() || !this.receivedConnection) {
                    this.selector.select();

                    Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();

                        try {
                            this.handleIncomingConnections(key);

                            this.handleIncomingMessages(key);

                            this.handleQueuedMessages(key);

                        } catch (MalformedFrameException | IllegalArgumentException | IllegalStateException e) {
                            Logger.logError("Exception encountered", e);

                            this.activeConnections.remove(this.getId(key));
                            key.channel().close();
                        }

                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                Logger.logError("IOException encountered", e);
            }

            this.shutdown();
        }
    }

    private void handleIncomingConnections(SelectionKey key) throws IOException {
        if (key.isValid() && key.isAcceptable()) {
            SocketChannel connection = this.server.accept();

            if (connection != null) {
                ConnectionData data = new ConnectionData(this.utilities.getNextId());
                this.activeConnections.put(data.getConnectionId(), data);

                connection.configureBlocking(false);
                connection.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, data.getConnectionId());

                this.receivedConnection = true;
            }
        }
    }

    private void handleIncomingMessages(SelectionKey key) throws IOException {
        if (key.isValid() && key.isReadable()) {
            SocketChannel connection = this.getChannel(key);
            ConnectionData data = this.activeConnections.get(this.getId(key));

            if (data.wasUpgraded()) {
                FrameData frameData = this.utilities.readMetadata(connection);
                String message = this.utilities.unmaskMessage(connection, frameData);

                Logger.log(message);

                switch (frameData.getOpcode()) {
                    case 0, 1 -> this.handleMessage(frameData, message);

                    case 8 -> this.handleCloseRequest(frameData, message);

                    case 9 -> this.handlePingRequest(frameData, message);

                    default -> this.handleUnsupportedOpcode(frameData);
                }
            }

            if (!data.wasUpgraded()) {
                boolean result = this.utilities.upgradeConnection(connection);

                if (result) {
                    data.setWasUpgraded(true);
                }
            }
        }
    }

    private void handleQueuedMessages(SelectionKey key) throws IOException {
        if (key.isValid() && key.isWritable()) {
            ConnectionData data = this.activeConnections.get(this.getId(key));
            String pendingMessage = data.pollMessage();

            if (pendingMessage != null) {
                SocketChannel connection = this.getChannel(key);

                ByteBuffer buffer = FrameBuilder.buildFrame(pendingMessage, true, 1);
                ChannelHelper.writeBytes(connection, buffer);
            }
        }
    }

    private void handleCloseRequest(FrameData frameData, String message) {
        //TODO: implement
    }

    private void handlePingRequest(FrameData frameData, String message) {
        //TODO: implement
    }

    private void handleMessage(FrameData frameData, String message) {
        //TODO: implement
    }

    private void handleUnsupportedOpcode(FrameData frameData) {
        Logger.logAsError("Unsupported opcode!");
        //TODO: send error to client
    }

    private SocketChannel getChannel(SelectionKey key) {
        return (SocketChannel) key.channel();
    }

    private long getId(SelectionKey key) {
        return (long) key.attachment();
    }

    private boolean isRunning() {
        return this.server != null && this.server.isOpen()
                && this.selector != null && this.selector.isOpen();
    }

    //TODO: refactor old nio code, to close channels properly
    private void shutdown() {
        try {
            System.out.println("Starting shutdown process...");

            if (this.selector != null) {
                for (SelectionKey key : this.selector.keys()) {
                    key.channel().close();
                }

                this.selector.close();
            }
        } catch (IOException e) {
            Logger.logError("Server encountered exception while shutting down", e);
        }
    }

    public static void main(String[] args) {
        DemoServer server = new DemoServer(80);
        server.start();
    }
}