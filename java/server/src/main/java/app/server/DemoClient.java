package app.server;

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

        WebSocket ws = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:80"), new WebSocketClient())
                .join();

        String message;

        while (!"QUIT".equals(message = reader.readLine())) {
            ws.sendText(message, true);
        }

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "User decided to quit");
    }

    private static class WebSocketClient implements WebSocket.Listener {
        public WebSocketClient() {
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("onOpen using subprotocol " + webSocket.getSubprotocol());
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("onText received " + data);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("Bad day! " + webSocket.toString());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}
