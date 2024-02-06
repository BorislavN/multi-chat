package app.server.demo.endpoint;

import app.server.demo.Constants;
import app.server.demo.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

//An approach to avoid buffering the fragmented messages at the server, wold be
//to define an extension, which adds a 2 byte messageId at the start of the payload
//and using a reserved bit / reserved opcode to signal the use of an extension.
//Although this sounds nice, the current java / javascript implementation does not allow to specify extensions
//before initiating the handshake. So what..., we will set a custom opcode without specifying an extension...
//Yea, and the implementations throw an "unrecognized frame opcode" exception :D
//Soo, there is no way to implement it currently, without writing my own implementation
//
//Another approach is to define some sub-protocol, for example one using JSON,
//with a field for messageId / other metadata, the server will send these JSONs as completed frames, and the client will
//interpret the metadata and act accordingly, even if there was an intermediary (there is not, we are running this in localhost), one that decides to split our frame,
//the client should buffer the fragmented frame before parsing the JSON, avoiding potential exceptions

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

                            //TODO: rework, we can send a close request in specific cases - like when the message is too large etc...
                            // in some cases we will only close the connection and log the error, as the other side of the channel could be closed
                            ByteBuffer buffer = FrameBuilder.buildCloseFrame(1006, e.getMessage());

                            ChannelHelper.writeBytes((SocketChannel) key.channel(), buffer);

                            this.disconnectUser(key);
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
                long nextId = this.utilities.getNextId();
                connection.configureBlocking(false);

                SelectionKey channelKey = connection.register(this.selector, SelectionKey.OP_READ, nextId);
                ConnectionData data = new ConnectionData(channelKey);

                this.activeConnections.put(nextId, data);
                this.receivedConnection = true;
            }
        }
    }

    private void handleIncomingMessages(SelectionKey key) throws IOException {
        if (key.isValid() && key.isReadable()) {
            SocketChannel connection = this.getChannel(key);
            ConnectionData connectionData = this.activeConnections.get(this.getId(key));

            if (connectionData.wasUpgraded()) {
                this.utilities.readFrame(connectionData);

                FrameData lastFrame = connectionData.getLastFrame();

                if (lastFrame.isReadCompleted()) {
                    Logger.log(lastFrame.getMessage());

                    switch (lastFrame.getOpcode()) {
                        case 0, 1 -> this.handleMessage(connectionData, lastFrame);

                        case 8 -> this.handleCloseRequest(connectionData, lastFrame);

                        case 9 -> this.handlePingRequest(connectionData, lastFrame);
                    }
                }
            }

            if (!connectionData.wasUpgraded()) {
                boolean result = this.utilities.upgradeConnection(connection);

                if (result) {
                    connectionData.setWasUpgraded(true);
                }
            }
        }
    }

    private void handleQueuedMessages(SelectionKey key) throws IOException {
        if (key.isValid() && key.isWritable()) {
            ConnectionData data = this.activeConnections.get(this.getId(key));

            if (data == null) {
                Logger.logAsError("Connection not in list of active connections!");

                return;
            }

            ByteBuffer pendingFrame = data.pollFrame();

            if (pendingFrame != null) {
                SocketChannel connection = this.getChannel(key);
                pendingFrame.position(0);

                ChannelHelper.writeBytes(connection, pendingFrame);

                if (this.wasCloseFrame(pendingFrame.get(0)) && data.receivedClose()) {
                    this.disconnectUser(key);
                }
            }
        }
    }

    private void handleCloseRequest(ConnectionData connectionData, FrameData frameData) {
        if (this.isFragment(frameData)) {
            throw new IllegalStateException("Control frames cannot be fragmented!");
        }

        connectionData.setReceivedClose(true);

        connectionData.enqueuePriorityMessage(FrameBuilder.buildFrame(frameData));
    }

    private void handlePingRequest(ConnectionData connectionData, FrameData frameData) {
        if (this.isFragment(frameData)) {
            throw new IllegalStateException("Control frames cannot be fragmented!");
        }

        connectionData.setReceivedPing(true);

        connectionData.enqueuePriorityMessage(FrameBuilder.buildPongFrame(frameData));
    }

    private void handleMessage(ConnectionData connectionData, FrameData frameData) {
        ByteBuffer frame = FrameBuilder.buildFrame(frameData);

        if (this.isFragment(frameData)) {
            connectionData.addFragment(frame);

            if (frameData.isFinished()) {
                this.enqueueFragmentsToAllUsers(connectionData);
            }

            return;
        }

        if (frameData.getMessage().startsWith(Constants.USERNAME_COMMAND)) {
            String name = frameData.getMessage().substring(Constants.USERNAME_COMMAND.length());
            ByteBuffer signalFrame = FrameBuilder.buildFrame(true, 1, Constants.USERNAME_COMMAND.getBytes(StandardCharsets.UTF_8));

            if (name.length() < Constants.MIN_USERNAME_LENGTH) {
                connectionData.enqueuePriorityMessage(signalFrame);
                connectionData.enqueuePriorityMessage(FrameBuilder.buildFrame(true, 1, "Username too short!".getBytes(StandardCharsets.UTF_8)));

                return;
            }

            for (ConnectionData data : this.activeConnections.values()) {
                if (!connectionData.equals(data) && data.getUsername().equals(name)) {
                    connectionData.enqueuePriorityMessage(signalFrame);
                    connectionData.enqueuePriorityMessage(FrameBuilder.buildFrame(true, 1, "Username taken!".getBytes(StandardCharsets.UTF_8)));

                    return;
                }
            }

            String oldName = connectionData.getUsername();

            connectionData.enqueuePriorityMessage(frame);

            if (!name.equals(oldName)) {
                String message = String.format("%s joined the chat!", name);

                if (oldName != null) {
                    message = String.format("%s changed their name to %s.", oldName, name);
                }

                connectionData.setUsername(name);

                this.enqueueToAllUsers(FrameBuilder.buildFrame(true, 1, message.getBytes(StandardCharsets.UTF_8)));
            }

            return;
        }

        this.enqueueToAllUsers(frame);
    }

    private boolean isFragment(FrameData frameData) {
        return !frameData.isFinished() || frameData.getOpcode() == 0;
    }

    private void enqueueFragmentsToAllUsers(ConnectionData connectionData) {
        List<ByteBuffer> fragments = connectionData.getFragments();

        for (ConnectionData value : this.activeConnections.values()) {
            value.enqueueFragments(fragments);
        }

        connectionData.clearFragments();
    }

    private void enqueueToAllUsers(ByteBuffer frame) {
        for (ConnectionData value : this.activeConnections.values()) {
            value.enqueueMessage(frame);
        }
    }

    private boolean isUsernameAvailable(String username) {
        for (ConnectionData value : this.activeConnections.values()) {
            if (value.getUsername().equals(username)) {
                return false;
            }
        }

        return true;
    }

    private void disconnectUser(SelectionKey key) {
        try {
            long connectionId = this.getId(key);

            key.cancel();
            key.channel().close();
            this.activeConnections.remove(connectionId);

        } catch (IOException e) {
            Logger.logError("Encountered exception while attempting to close channel", e);
        }
    }

    private boolean wasCloseFrame(byte value) {
        return (value & 0b1111) == 8;
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