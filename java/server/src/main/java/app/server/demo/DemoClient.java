package app.server.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

//Example from - https://stackoverflow.com/questions/55380813/require-assistance-with-simple-pure-java-11-websocket-client-example
//Using java.net.http.WebSocket
public class DemoClient {
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

        client.sendClose(WebSocket.NORMAL_CLOSURE, "User decided to quit");
    }

    private static class WebSocketClient implements WebSocket.Listener {
        public WebSocketClient() {
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Connection established!");
            System.out.println("Sub-protocol: " + webSocket.getSubprotocol());

            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Message received: " + data);

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("Exception occurred: " + error.getMessage());

            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}
