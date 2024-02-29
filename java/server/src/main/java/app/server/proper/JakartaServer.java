package app.server.proper;

import app.server.WebsocketServer;
import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JakartaServer implements WebsocketServer {
    private final Server server;

    public JakartaServer(int port) {
        this.server = new Server("localhost", port, "/", null, JakartaListener.class);
    }

    @Override
    public void start() {
        try {
            this.server.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please press a key to stop the server.");
            reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.server.stop();
        }
    }
}
