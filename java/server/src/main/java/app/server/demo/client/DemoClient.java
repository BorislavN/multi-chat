package app.server.demo.client;

import app.server.demo.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;

//Example from - https://stackoverflow.com/questions/55380813/require-assistance-with-simple-pure-java-11-websocket-client-example
//Using java.net.http.WebSocket

//TODO: refactor
// implement fragmented message concatenation in js client too
public class DemoClient {
    private static volatile boolean closeInitiated = false;
    private static final Timer timer = new Timer();

    public static void main(String[] args) throws Exception {
//        System.setProperty("jdk.internal.httpclient.websocket.debug", "true");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        WebSocket client = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:80"), new WebSocketClient())
                .join();

        String message;
        String username = "";

        System.out.println("Choose an username:");

        while (username.isBlank()) {
            username = reader.readLine();

            if (username.isBlank()) {
                System.out.println("Username cannot be blank!");
            }
        }

        System.out.println("Welcome to the chat!");
        System.out.println("Enter \"QUIT\" to exit.");
        System.out.println();

        while (!"QUIT".equals(message = reader.readLine())) {
            if (message.isBlank()) {
                System.out.println("Message cannot be blank!");

                continue;
            }

            client.sendText(String.format("%s: %s", username, message), true);
        }

        closeInitiated = true;
        client.sendClose(1000, "Java client wants to quit").thenRun(closeHandler(client));
    }

    private static Runnable closeHandler(WebSocket client) {
        return () -> {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    client.abort();
                }
            };

            timer.schedule(timerTask, 30000);
        };
    }

    private static class WebSocketClient implements WebSocket.Listener {
        private StringBuilder stringBuilder;

        public WebSocketClient() {
            this.stringBuilder = new StringBuilder();
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Connection established!");

            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            this.stringBuilder.append(data);

            if (last) {
                System.out.println(this.stringBuilder);
                this.stringBuilder = new StringBuilder();
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            Logger.logError("Exception occurred", error);
            Logger.logError("Cause", error.getCause());

//            error.printStackTrace();

            WebSocket.Listener.super.onError(webSocket, error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (reason.isBlank()) {
                reason = "None";
            }

            System.out.printf("Close frame received - Code: %d, Reason: %s%n", statusCode, reason);

            if (closeInitiated) {
                timer.cancel();
                webSocket.abort();

                return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
            }

            //Echo back close frame
            webSocket.sendClose(statusCode, reason).thenRun(closeHandler(webSocket));

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}