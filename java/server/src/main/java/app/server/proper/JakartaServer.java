package app.server.proper;

import app.server.WebsocketServer;
import app.util.Logger;
import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

public class JakartaServer implements WebsocketServer {
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
            this.server.stop();
        }
    }

    @Override
    public void shutdown() {
        this.server.stop();
    }
}
