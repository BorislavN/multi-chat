package app.server.minimal;

import app.server.UsernameStatus;
import app.server.WebsocketServer;
import app.server.minimal.entity.ConnectionData;
import app.server.minimal.entity.FrameData;
import app.server.minimal.entity.UpgradeStatus;
import app.server.minimal.exception.ConnectionException;
import app.server.minimal.exception.MalformedFrameException;
import app.server.minimal.exception.MessageLengthException;
import app.server.minimal.helper.ChannelHelper;
import app.server.minimal.helper.FrameBuilder;
import app.server.minimal.helper.ServerUtil;
import app.util.Constants;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static app.util.Constants.COMMAND_DELIMITER;
import static app.util.Constants.USERNAME_TAKEN;

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

public class JavaServer implements WebsocketServer {
    private ServerSocketChannel server;
    private Selector selector;
    private final ServerUtil utilities;
    private final Map<Long, ConnectionData> activeConnections;
    private final AtomicBoolean isRunning;
    private Thread workerThread;

    public JavaServer(String host, int port) {
        this.activeConnections = new HashMap<>();
        this.isRunning = new AtomicBoolean(true);
        this.utilities = new ServerUtil();
        this.workerThread = null;

        try {
            this.server = ServerSocketChannel.open();
            this.server.bind(new InetSocketAddress(host, port));
            this.server.configureBlocking(false);

            System.out.printf("JavaServer starting on %s:%d...%n", host, port);

            this.selector = Selector.open();
            this.server.register(this.selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            Logger.logError("JavaServer failed to start up", e);
            this.isRunning.set(false);
        }
    }

    @Override
    public void start() {
        if (!this.isRunning.get()) {
            return;
        }

        this.workerThread = new Thread(() -> {
            while (this.isRunning.get()) {
                this.selectorSelect();

                Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    try {
                        this.handleIncomingConnections(key);

                        this.handleIncomingMessages(key);

                        this.handleQueuedMessages(key);

                    } catch (MalformedFrameException e) {
                        this.terminateConnection(key, true, 1002, e.getMessage());

                    } catch (MessageLengthException e) {
                        this.terminateConnection(key, true, 1009, e.getMessage());

                    } catch (IllegalStateException e) {
                        this.terminateConnection(key, true, 1011, e.getMessage());

                    } catch (IOException | ConnectionException e) {
                        Logger.logError("Exception encountered", e);

                        this.disconnectUser(key, true);
                    }

                    iterator.remove();
                }
            }

            System.out.println("Listener thread shutting down...");
        });

        this.workerThread.start();
    }

    @Override
    public void shutdown() {
        System.out.println("Starting shutdown process...");

        if (this.workerThread == null) {
            return;
        }

        this.isRunning.set(false);
        this.selector.wakeup();

        try {
            Iterator<Map.Entry<Long, ConnectionData>> iterator = this.activeConnections.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, ConnectionData> connection = iterator.next();

                this.terminateConnection(connection.getValue().getSelectionKey(), false, 1001, "Server shutting down!");

                iterator.remove();
            }

            System.out.println("Connections left: " + this.activeConnections.size());

            this.selector.close();
            this.server.close();

        } catch (IOException e) {
            Logger.logError("Server encountered exception while shutting down", e);
        } finally {
            if (this.workerThread.isAlive()) {
                System.out.println("Thread still running - interrupting...");
                this.workerThread.interrupt();
            }
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

                FrameData lastFrame = connectionData.getCurrentFrame();

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
                    this.disconnectUser(key, true);

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

                if (this.wasCloseFrame(pendingFrame.get(0))) {
                    if (data.receivedClose() && data.sentClose()) {
                        this.disconnectUser(key, true);
                    }
                }
            }
        }
    }

    private void handleCloseRequest(ConnectionData connectionData) {
        this.validateControlFrame(connectionData.getCurrentFrame());

        connectionData.setReceivedClose(true);

        if (connectionData.receivedClose() && connectionData.sentClose()) {
            this.disconnectUser(connectionData.getSelectionKey(), true);

            return;
        }

        connectionData.enqueuePriorityMessage(FrameBuilder.copyFrame(connectionData.getCurrentFrame()));
        connectionData.setSentClose(true);
    }

    private void handlePingRequest(ConnectionData connectionData) {
        this.validateControlFrame(connectionData.getCurrentFrame());

        connectionData.enqueuePriorityMessage(FrameBuilder.buildPongFrame(connectionData.getCurrentFrame()));
    }

    private void handleMessage(ConnectionData connectionData) {
        ByteBuffer frame = FrameBuilder.copyFrame(connectionData.getCurrentFrame());

        if (this.handleFragment(connectionData, frame)) {
            return;
        }

        if (this.handleUsernameSelection(connectionData)) {
            return;
        }

        this.enqueueToAllUsers(frame);
    }

    private void terminateConnection(SelectionKey key, boolean removeEntry, int code, String reason) {
        SocketChannel connection = (SocketChannel) key.channel();
        ByteBuffer closeFrame = FrameBuilder.buildCloseFrame(code, reason);

        try {
            ChannelHelper.writeBytes(connection, closeFrame);
        } catch (IOException e) {
            Logger.logAsError("Exception occurred while sending Close-frame!");
        }

        this.disconnectUser(key, removeEntry);
    }

    private void disconnectUser(SelectionKey key, boolean removeEntry) {
        try {
            long connectionId = this.getId(key);

            key.cancel();
            key.channel().close();

            if (removeEntry) {
                ConnectionData removed = this.activeConnections.remove(connectionId);

                if (this.isRunning.get() && removed.getUsername() != null) {
                    ByteBuffer announcement = FrameBuilder.buildTextFrame(Constants.newLeftAnnouncement(removed.getUsername()));

                    this.enqueueToAllUsers(announcement);
                }
            }

        } catch (IOException e) {
            Logger.logError("Encountered exception while attempting to close channel", e);
        }
    }

    private boolean handleFragment(ConnectionData connectionData, ByteBuffer frame) {
        if (this.isFragment(connectionData.getCurrentFrame())) {
            connectionData.addFragment(frame);

            if (connectionData.getCurrentFrame().isFinished()) {
                this.enqueueFragmentsToAllUsers(connectionData);
            }

            return true;
        }

        return false;
    }

    private boolean handleUsernameSelection(ConnectionData connectionData) {
        FrameData lastFrame = connectionData.getCurrentFrame();

        if (lastFrame.getMessage().startsWith(COMMAND_DELIMITER)) {
            String responseText = null;
            String announcement = null;

            UsernameStatus status = new UsernameStatus(lastFrame.getMessage());

            if (!status.isValid()) {
                responseText = status.getError();
            }

            if (status.isValid() && !this.isUsernameAvailable(connectionData, status.getUsername())) {
                responseText = USERNAME_TAKEN;
            }

            if (responseText == null) {
                responseText = Constants.newAcceptedResponse(status.getUsername());

                String oldName = connectionData.getUsername();

                announcement = UsernameStatus.newUsernameSetAnnouncement(oldName, status.getUsername());
            }

            ByteBuffer response = FrameBuilder.buildTextFrame(responseText);
            connectionData.enqueueMessage(response);

            if (announcement != null) {
                connectionData.setUsername(status.getUsername());

                ByteBuffer announcementFrame = FrameBuilder.buildTextFrame(announcement);
                this.enqueueToAllUsers(announcementFrame);
            }

            return true;
        }

        return false;
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
            if (value.getUsername() != null) {
                value.enqueueMessage(frame);
            }
        }
    }

    private boolean isUsernameAvailable(ConnectionData current, String username) {
        for (ConnectionData value : this.activeConnections.values()) {
            if (value != current && username.equals(value.getUsername())) {
                return false;
            }
        }

        return true;
    }

    private boolean isFragment(FrameData frameData) {
        return !frameData.isFinished() || frameData.getOpcode() == 0;
    }

    private boolean wasCloseFrame(byte value) {
        return (value & 0b1111) == 8;
    }

    private void validateControlFrame(FrameData frame) {
        if (this.isFragment(frame)) {
            throw new IllegalStateException("Control frames cannot be fragmented!");
        }
    }

    private SocketChannel getChannel(SelectionKey key) {
        return (SocketChannel) key.channel();
    }

    private long getId(SelectionKey key) {
        return (long) key.attachment();
    }
}