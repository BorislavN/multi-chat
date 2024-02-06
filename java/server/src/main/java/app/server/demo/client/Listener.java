package app.server.demo.client;

import app.server.demo.Constants;
import app.server.demo.Logger;

import java.net.http.WebSocket;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Listener implements WebSocket.Listener {
    private volatile String username;
    private volatile boolean closeInitiated;
    private StringBuilder stringBuilder;
    private CompletableFuture<?> messageStage;
    private final Timer timer;
    private final Object lock;

    public Listener(Timer timer, Object lock) {
        this.stringBuilder = new StringBuilder();
        this.messageStage = new CompletableFuture<>();
        this.username = "";
        this.closeInitiated = false;
        this.timer = timer;
        this.lock = lock;
    }

    public boolean isCloseInitiated() {
        return this.closeInitiated;
    }

    public void setCloseInitiated(boolean closeInitiated) {
        this.closeInitiated = closeInitiated;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("Connection established!");
        System.out.printf("Enter \"%s\" to choose an username (without space).%n", Constants.USERNAME_COMMAND);
        System.out.println("Enter \"QUIT\" to exit.");
        System.out.println();

        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        if (message.startsWith(Constants.USERNAME_COMMAND)) {
            String name = message.substring(Constants.USERNAME_COMMAND.length());

            synchronized (this.lock) {
                if (!name.isBlank()) {
                    this.username = name;
                }

                this.lock.notify();
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        this.stringBuilder.append(message);
        webSocket.request(1);

        if (last) {
            System.out.println(this.stringBuilder);

            this.stringBuilder = new StringBuilder();
            this.messageStage.complete(null);

            CompletionStage<?> currentStage = this.messageStage;
            this.messageStage = new CompletableFuture<>();

            return currentStage;
        }

        return this.messageStage;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logger.logError("Exception occurred", error);
        Logger.logError("Cause", error.getCause());
        System.out.println("Please enter \"QUIT\" to exit the application!");

//            error.printStackTrace();

        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (reason.isBlank()) {
            reason = "None";
        }

        System.out.printf("Close frame received - Code: %d, Reason: %s%n", statusCode, reason);

        if (this.closeInitiated) {
            this.timer.cancel();
            webSocket.abort();
        } else {
            System.out.println("Please enter \"QUIT\" to exit the application!");
        }

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);

        //Echo back close frame
//            webSocket.sendClose(statusCode, reason).thenRun(closeHandler(webSocket));
    }
}