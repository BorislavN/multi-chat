package app.server.demo.endpoint;

import java.nio.ByteBuffer;
import java.util.*;

public class ConnectionData {
    private long connectionId;
    private String username;
    private boolean wasUpgraded;
    private boolean receivedPing;
    private boolean receivedClose;
    private final List<ByteBuffer> fragments;
    private final Deque<ByteBuffer> waitingFrames;

    public ConnectionData(long connectionId) {
        this.connectionId = connectionId;
        this.username = null;
        this.wasUpgraded = false;
        this.receivedPing = false;
        this.receivedClose = false;
        this.waitingFrames = new ArrayDeque<>();
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

    public ByteBuffer pollFrame() {
        return this.waitingFrames.poll();
    }

    public void enqueueMessage(ByteBuffer frame) {
        this.waitingFrames.offer(frame);
    }

    public void enqueuePriorityMessage(ByteBuffer frame) {
        this.waitingFrames.offerFirst(frame);
    }

    public List<ByteBuffer> getFragments() {
        return Collections.unmodifiableList(this.fragments);
    }

    public void addFragment(ByteBuffer fragment) {
        this.fragments.add(fragment);
    }

    public void enqueueFragments(List<ByteBuffer> fragments) {
        this.waitingFrames.addAll(fragments);
    }

    public void clearFragments() {
        this.fragments.clear();
    }
}