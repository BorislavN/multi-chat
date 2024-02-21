package app.server.minimal;

import app.server.minimal.entity.ConnectionData;
import app.server.minimal.entity.FrameData;
import app.server.minimal.entity.UpgradeStatus;
import app.server.minimal.exception.ConnectionException;
import app.server.minimal.exception.MalformedFrameException;
import app.server.minimal.exception.MessageLengthException;
import app.server.minimal.helper.ChannelHelper;
import app.server.minimal.helper.FrameBuilder;
import app.server.minimal.helper.ServerUtil;
import app.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static app.util.Constants.*;
import static java.nio.charset.StandardCharsets.UTF_8;

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

//TODO: needs refactoring
// 1: ping/pong functionality
// 2: frame building
// 3: spamming handling?
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
            while (!this.activeConnections.isEmpty() || !this.receivedConnection) {
                this.selectorSelect();

                Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    try {
                        this.handleIncomingConnections(key);

                        this.handleIncomingMessages(key);

                        this.handleQueuedMessages(key);

                    } catch (MalformedFrameException e) {
                        this.terminateConnection(key, 1002, e.getMessage());
                    } catch (MessageLengthException e) {
                        this.terminateConnection(key, 1009, e.getMessage());
                    } catch (IllegalStateException e) {
                        this.terminateConnection(key, 1011, e.getMessage());
                    } catch (IOException | ConnectionException e) {
                        Logger.logError("Exception encountered", e);
                        this.disconnectUser(key);
                    }

                    iterator.remove();
                }
            }

            this.shutdown();
        }
    }

    private void selectorSelect() {
        try {
            this.selector.select();
        } catch (IOException exception) {
            Logger.logAsError("Selection failed!");
        }
    }

    private void handleIncomingConnections(SelectionKey key) {
        if (key.isValid() && key.isAcceptable()) {
            try {
                SocketChannel connection = this.server.accept();

                if (connection != null) {
                    long nextId = this.utilities.getNextId();
                    connection.configureBlocking(false);

                    SelectionKey channelKey = connection.register(this.selector, SelectionKey.OP_READ, nextId);
                    ConnectionData data = new ConnectionData(channelKey);

                    this.activeConnections.put(nextId, data);
                    this.receivedConnection = true;
                }
            } catch (IOException e) {
                Logger.logAsError("Exception encountered while handling incoming connection!");
            }
        }
    }

    private void handleIncomingMessages(SelectionKey key) {
        if (key.isValid() && key.isReadable()) {
            SocketChannel connection = this.getChannel(key);
            ConnectionData connectionData = this.activeConnections.get(this.getId(key));

            if (connectionData.wasUpgraded()) {
                this.utilities.readFrame(connectionData);

                FrameData lastFrame = connectionData.getLastFrame();

                if (lastFrame.isReadCompleted()) {
                    Logger.log(lastFrame.getMessage());

                    switch (lastFrame.getOpcode()) {
                        case 0, 1 -> this.handleMessage(connectionData);

                        case 8 -> this.handleCloseRequest(connectionData);

                        case 9 -> this.handlePingRequest(connectionData);
                    }
                }
            }

            if (!connectionData.wasUpgraded()) {
                UpgradeStatus result = this.utilities.upgradeConnection(connection);

                if (result.threwException()) {
                    this.disconnectUser(key);

                    return;
                }

                if (result.wasUpgraded()) {
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

    private void handleCloseRequest(ConnectionData connectionData) {
        if (this.isFragment(connectionData.getLastFrame())) {
            throw new IllegalStateException("Control frames cannot be fragmented!");
        }

        connectionData.setReceivedClose(true);
        connectionData.enqueuePriorityMessage(FrameBuilder.buildFrame(connectionData.getLastFrame()));
    }

    private void handlePingRequest(ConnectionData connectionData) {
        if (this.isFragment(connectionData.getLastFrame())) {
            throw new IllegalStateException("Control frames cannot be fragmented!");
        }

        connectionData.setReceivedPing(true);

        connectionData.enqueuePriorityMessage(FrameBuilder.buildPongFrame(connectionData.getLastFrame()));
    }

    private void handleMessage(ConnectionData connectionData) {
        ByteBuffer frame = FrameBuilder.buildFrame(connectionData.getLastFrame());

        if (this.handleFragment(connectionData, frame)) {
            return;
        }

        if (this.handleUsernameSelection(connectionData)) {
            return;
        }

        this.enqueueToAllUsers(frame);
    }

    private void terminateConnection(SelectionKey key, int code, String reason) {
        SocketChannel connection = (SocketChannel) key.channel();
        ByteBuffer closeFrame = FrameBuilder.buildCloseFrame(code, reason);

        try {
            ChannelHelper.writeBytes(connection, closeFrame);
        } catch (IOException e) {
            Logger.logAsError("Exception occurred while sending Close-frame!");
        }

        this.disconnectUser(key);
    }

    private boolean handleFragment(ConnectionData connectionData, ByteBuffer frame) {
        if (this.isFragment(connectionData.getLastFrame())) {
            connectionData.addFragment(frame);

            if (connectionData.getLastFrame().isFinished()) {
                this.enqueueFragmentsToAllUsers(connectionData);
            }

            return true;
        }

        return false;
    }

    //TODO: debug, this stopped working properly
    private boolean handleUsernameSelection(ConnectionData connectionData) {
        FrameData lastFrame = connectionData.getLastFrame();

        if (lastFrame.getMessage().startsWith(COMMAND_DELIMITER)) {
            String name = lastFrame.getMessage().substring(COMMAND_DELIMITER.length());
            String responseText = null;
            String announcement = null;

            if (name.length() < MIN_USERNAME_LENGTH) {
                responseText = String.format("%s%sMust be least %d chars!", EXCEPTION_FLAG, COMMAND_DELIMITER, MIN_USERNAME_LENGTH);
            }

            if (responseText == null && !this.isUsernameAvailable(connectionData, name)) {
                responseText = String.format("%s%sUsername is taken!", EXCEPTION_FLAG, COMMAND_DELIMITER);
            }

            if (responseText == null) {
                responseText = String.format("%s%s%s", ACCEPTED_FLAG, COMMAND_DELIMITER, name);

                String oldName = connectionData.getUsername();

                if (!name.equals(oldName)) {
                    announcement = String.format("\"%s\" joined the chat!", name);

                    if (oldName != null) {
                        announcement = String.format("\"%s\" changed their name to \"%s\".", oldName, name);
                    }

                    connectionData.setUsername(name);
                }
            }

            ByteBuffer response = FrameBuilder.buildFrame(true, 1, responseText.getBytes(UTF_8));
            connectionData.enqueueMessage(response);

            if (announcement != null) {
                ByteBuffer announcementFrame = FrameBuilder.buildFrame(true, 1, responseText.getBytes(UTF_8));
                connectionData.enqueueMessage(announcementFrame);
            }

            return true;
        }

        return false;
    }

    private boolean isFragment(FrameData frameData) {
        return !frameData.isFinished() || frameData.getOpcode() == 0;
    }

    private boolean isUsernameAvailable(ConnectionData connectionData, String username) {
        for (ConnectionData value : this.activeConnections.values()) {
            if (value != connectionData && username.equals(value.getUsername())) {
                return false;
            }
        }

        return true;
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

    private void disconnectUser(SelectionKey key) {
        try {
            long connectionId = this.getId(key);

            key.cancel();
            key.channel().close();
            ConnectionData removed = this.activeConnections.remove(connectionId);

            if (removed.getUsername() != null) {
                ByteBuffer announcement = FrameBuilder.buildFrame(
                        true,
                        1,
                        String.format("\"%s\" left the chat...", removed.getUsername()).getBytes(UTF_8)
                );

                this.enqueueToAllUsers(announcement);
            }

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