package app.server.demo.client;

import app.server.demo.Logger;

import java.net.http.WebSocket;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static app.server.demo.Constants.*;

public class Listener implements WebSocket.Listener {
    private volatile boolean usernameApproved;
    private volatile boolean closeInitiated;
    private StringBuilder stringBuilder;
    private CompletableFuture<?> messageStage;
    private final Timer timer;
    private final Object lock;

    public Listener(Timer timer, Object lock) {
        this.stringBuilder = new StringBuilder();
        this.messageStage = new CompletableFuture<>();
        this.usernameApproved = false;
        this.closeInitiated = false;
        this.timer = timer;
        this.lock = lock;
    }

    public boolean isUsernameApproved() {
        return this.usernameApproved;
    }

    public boolean isCloseInitiated() {
        return this.closeInitiated;
    }

    public void setCloseInitiated(boolean closeInitiated) {
        this.closeInitiated = closeInitiated;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("Connection established!");
        System.out.printf("Enter \"%sName\" to choose an username.%n", COMMAND_DELIMITER);
        System.out.println("Enter \"QUIT\" to exit.");
        System.out.println();

        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        boolean isAccepted = message.startsWith(ACCEPTED_FLAG.concat(COMMAND_DELIMITER));
        boolean isError = message.startsWith(ERROR_FLAG.concat(COMMAND_DELIMITER));

        if (isError || isAccepted) {
            String[] parts = message.split(COMMAND_DELIMITER);

            synchronized (this.lock) {
                if (isAccepted) {
                    this.usernameApproved = true;
                }

                if (isError) {
                    this.usernameApproved = false;
                    System.out.println(parts[1]);
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
            this.closeInitiated = true;
        }

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}