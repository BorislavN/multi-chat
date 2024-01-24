package app.server.demo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ConnectionData {
    private long connectionId;
    private String username;
    private boolean wasUpgraded;
    private boolean receivedPing;
    private boolean receivedClose;
    private final List<StringBuilder> fragments;
    private final Deque<String> waitingMessages;

    public ConnectionData(long connectionId) {
        this.connectionId = connectionId;
        this.username = null;
        this.wasUpgraded = false;
        this.receivedPing = false;
        this.receivedClose = false;
        this.waitingMessages = new ArrayDeque<>();
        this.fragments = new ArrayList<>();
    }

    public long getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean wasUpgraded() {
        return this.wasUpgraded;
    }

    public void setWasUpgraded(boolean wasUpgraded) {
        this.wasUpgraded = wasUpgraded;
    }

    public boolean receivedPing() {
        return this.receivedPing;
    }

    public void setReceivedPing(boolean receivedPing) {
        this.receivedPing = receivedPing;
    }

    public boolean receivedClose() {
        return this.receivedClose;
    }

    public void setReceivedClose(boolean receivedClose) {
        this.receivedClose = receivedClose;
    }

    //TODO: implement
    public String pollMessage() {
        return this.waitingMessages.poll();
    }

    //TODO: implement
    public void enqueueMessage(String message) {
        this.waitingMessages.offer(message);
    }

    //TODO: implement
    public List<StringBuilder> getFragments() {
        return this.fragments;
    }
}