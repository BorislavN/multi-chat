package app.server.proper;

import app.server.WebsocketServer;
import app.util.Logger;
import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

import java.util.concurrent.CopyOnWriteArraySet;

public class JakartaServer implements WebsocketServer {
    private final static CopyOnWriteArraySet<JakartaListener> connectedUsers = new CopyOnWriteArraySet<>();
    private final Server server;

    public JakartaServer(String host, int port) {
        this.server = new Server(host, port, "/", null, JakartaListener.class);
    }

    @Override
    public void start() {
        try {
            this.server.start();
        } catch (DeploymentException e) {
            Logger.logAsError("Server failed to start up!");

            clearConnections();

            this.server.stop();
        }
    }

    @Override
    public void shutdown() {
        disconnectClients();
        this.server.stop();
    }

    public static void forwardMessage(String message) {
        for (JakartaListener endpoint : connectedUsers) {
            if (endpoint.getUsername() != null) {
                endpoint.sendAsync(message);
            }
        }
    }

    public static void disconnectClients() {
        for (JakartaListener endpoint : connectedUsers) {
            endpoint.disconnect(1001, "Server shutting down!");
        }
    }

    public static boolean isUsernameAvailable(JakartaListener current, String name) {
        for (JakartaListener connection : connectedUsers) {
            if (connection != current && name.equals(connection.getUsername())) {
                return false;
            }
        }

        return true;
    }

    public static void addConnection(JakartaListener connection) {
        connectedUsers.add(connection);
    }

    public static void removeConnection(JakartaListener connection) {
        connectedUsers.remove(connection);
    }

    public static void clearConnections() {
        connectedUsers.clear();
    }
}
