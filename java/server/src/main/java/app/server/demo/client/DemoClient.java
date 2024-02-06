package app.server.demo.client;

import app.server.demo.Constants;
import app.server.demo.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;

//Example from - https://stackoverflow.com/questions/55380813/require-assistance-with-simple-pure-java-11-websocket-client-example
//Using java.net.http.WebSocket

//TODO: refactor
public class DemoClient {
    private final BufferedReader bufferedReader;
    private final WebSocket webSocket;
    private final Listener listener;
    private final Timer timer;
    private final Object lock;

    public DemoClient(int port) {
//        System.setProperty("jdk.internal.httpclient.websocket.debug", "true");

        this.bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        this.timer = new Timer();
        this.lock = new Object();

        this.listener = new Listener(this.timer, this.lock);
        this.webSocket = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(String.format("ws://localhost:%d", port)), this.listener)
                .join();
    }

    public void start() throws IOException {
        String message;
        String namePrefix = Constants.USERNAME_COMMAND;

        while (!"QUIT".equals(message = this.bufferedReader.readLine())) {
            if (message.isBlank()) {
                System.out.println("Input cannot be blank!");
                continue;
            }

            if (message.startsWith(namePrefix)) {
                if ((message.length() - namePrefix.length()) < Constants.MIN_USERNAME_LENGTH) {
                    System.out.println("Username too short!");
                    continue;
                }

                synchronized (this.lock) {
                    this.webSocket.sendText(message, true);

                    try {
                        this.lock.wait();
                    } catch (InterruptedException e) {
                        Logger.logError("Wait for username confirmation was interrupted!", e);
                    }

                    continue;
                }
            }

            String currentName = this.listener.getUsername();

            if (currentName.isBlank()) {
                System.out.println("Cannot send messages, without first choosing an username!");
                continue;
            }

            this.webSocket.sendText(String.format("%s: %s", currentName, message), true);
        }

        this.listener.setCloseInitiated(true);
        this.webSocket.sendClose(1000, "Java client wants to quit").thenRun(closeTimer(this.webSocket));
    }

    private Runnable closeTimer(WebSocket client) {
        return () -> {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    client.abort();
                }
            };

            this.timer.schedule(timerTask, 30000);
        };
    }

    public static void main(String[] args) throws IOException {
        DemoClient client = new DemoClient(80);
        client.start();
    }
}