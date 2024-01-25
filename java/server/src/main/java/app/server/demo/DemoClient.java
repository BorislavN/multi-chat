package app.server.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//Example from - https://stackoverflow.com/questions/55380813/require-assistance-with-simple-pure-java-11-websocket-client-example
//Using java.net.http.WebSocket

public class DemoClient {
    private static volatile boolean closeInitiated = false;
    private static final Timer timer = new Timer();

    public static void main(String[] args) throws Exception {
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
        client.sendClose(3000, "TEST").thenRun(closeHandler(client));
    }

    private static Runnable closeHandler(WebSocket client) {
        return () -> {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + ": from within TimerTask");
                    client.abort();
                }
            };


            timer.schedule(timerTask, 5000);
            System.out.println(Thread.currentThread().getName() + ": from within closeHandler");
        };
    }

    private static class WebSocketClient implements WebSocket.Listener {
        public WebSocketClient() {
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Connection established!");

            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("last: "+last);
            System.out.println("Message received: " + data);

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
            Logger.logError("Exception occurred", error);

            WebSocket.Listener.super.onError(webSocket, error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("On close...");
            System.out.println("Status code: " + statusCode);
            System.out.println(Thread.currentThread().getName() + ": from within Websocket Listener");

            if (!closeInitiated) {
                CompletableFuture<Object> temp = new CompletableFuture<>();
                temp.completeExceptionally(new IllegalStateException("Unexpected - Server initiated close handshake!"));

                return temp;
            }

            timer.cancel();
            webSocket.abort();

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}