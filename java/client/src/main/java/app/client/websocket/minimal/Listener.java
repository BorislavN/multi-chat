package app.client.websocket.minimal;

import app.util.Logger;

import java.net.http.WebSocket;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static app.util.Constants.*;

public class Listener implements WebSocket.Listener {
    private volatile String username;
    private volatile boolean usernameApproved;
    private volatile boolean closeInitiated;
    private StringBuilder stringBuilder;
    private CompletableFuture<?> messageStage;
    private final Timer timer;

    public Listener(Timer timer) {
        this.username = null;
        this.stringBuilder = new StringBuilder();
        this.messageStage = new CompletableFuture<>();
        this.usernameApproved = false;
        this.closeInitiated = false;
        this.timer = timer;
    }

    public String getUsername() {
        return this.username;
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
        //TODO: enable join button if connection is established, show error if not

        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        //TODO: redirect to main page if username was accept, show error if not
        // append text to main page
        //this.textArea.appendText("")

        boolean isAccepted = message.startsWith(ACCEPTED_FLAG.concat(COMMAND_DELIMITER));
        boolean isError = message.startsWith(ERROR_FLAG.concat(COMMAND_DELIMITER));

        if (isError || isAccepted) {
            String[] parts = message.split(COMMAND_DELIMITER);

            if (isAccepted) {
                this.usernameApproved = true;
            }

            if (isError) {
                this.usernameApproved = false;
                System.out.println(parts[1]);
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
     //TODO: show error, disable buttons

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

            //TODO: show that the connection was closed, disable buttons

        } else {
            System.out.println("Please enter \"QUIT\" to exit the application!");
            this.closeInitiated = true;
        }

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}