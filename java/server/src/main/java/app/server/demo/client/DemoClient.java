package app.server.demo.client;

import app.server.demo.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;

import static app.server.demo.Constants.COMMAND_DELIMITER;

public class DemoClient {
    private final BufferedReader bufferedReader;
    private final WebSocket webSocket;
    private final Listener listener;
    private String username;
    private final Timer timer;
    private final Object lock;

    public DemoClient(int port) {
//        System.setProperty("jdk.internal.httpclient.websocket.debug", "true");

        this.bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        this.timer = new Timer();
        this.lock = new Object();
        this.username = null;

        this.listener = new Listener(this.timer, this.lock);
        this.webSocket = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(String.format("ws://localhost:%d", port)), this.listener)
                .join();
    }

    public void start() throws IOException {
        String message;

        while (!"QUIT".equals(message = this.bufferedReader.readLine())) {
            if (message.isBlank()) {
                System.out.println("Input cannot be blank!");
                continue;
            }

            if (message.startsWith(COMMAND_DELIMITER)) {
                synchronized (this.lock) {
                    this.webSocket.sendText(message, true);

                    try {
                        this.lock.wait(5000);

                        if (this.listener.isUsernameApproved()) {
                            this.username = message.substring(COMMAND_DELIMITER.length());
                        }
                    } catch (InterruptedException e) {
                        Logger.logError("Wait for username confirmation was interrupted!", e);
                    }
                }

                continue;
            }

            if (this.username == null) {
                System.out.println("Cannot send messages, without first choosing an username!");
                continue;
            }

            this.webSocket.sendText(String.format("%s: %s", this.username, message), true);
        }

        if (!this.listener.isCloseInitiated()) {
            this.listener.setCloseInitiated(true);
            this.webSocket.sendClose(1000, "Java client wants to quit").thenRun(closeTimer());
        }
    }

    private Runnable closeTimer() {
        return () -> {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    webSocket.abort();
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