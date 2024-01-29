package app.server.demo.endpoint;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_WRITE;

public class ConnectionData {
    private long connectionId;
    private String username;
    private boolean wasUpgraded;
    private boolean receivedPing;
    private boolean receivedClose;
    private SelectionKey selectionKey;
    private final List<ByteBuffer> fragments;
    private final Deque<ByteBuffer> waitingFrames;
    private final int fragmentedMessageLimit;

    public ConnectionData(long connectionId, int fragmentedMessageLimit) {
        this.connectionId = connectionId;
        this.username = null;
        this.wasUpgraded = false;
        this.receivedPing = false;
        this.receivedClose = false;
        this.waitingFrames = new ArrayDeque<>();
        this.fragments = new ArrayList<>();
        this.fragmentedMessageLimit = fragmentedMessageLimit;
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

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public int getFragmentedMessageLimit() {
        return this.fragmentedMessageLimit;
    }

    public ByteBuffer pollFrame() {
        ByteBuffer temp = this.waitingFrames.poll();

        if (temp == null) {
            this.selectionKey.interestOps(SelectionKey.OP_READ);
        }

        return temp;
    }

    public void enqueueMessage(ByteBuffer frame) {
        this.waitingFrames.offer(frame);
        this.registerForWrite();
    }

    public void enqueuePriorityMessage(ByteBuffer frame) {
        this.waitingFrames.offerFirst(frame);
        this.registerForWrite();
    }

    public List<ByteBuffer> getFragments() {
        return Collections.unmodifiableList(this.fragments);
    }

    public void addFragment(ByteBuffer fragment) {
        this.fragments.add(fragment);
    }

    public void enqueueFragments(List<ByteBuffer> fragments) {
        this.waitingFrames.addAll(fragments);
        this.registerForWrite();
    }

    public void clearFragments() {
        this.fragments.clear();
    }

    private void registerForWrite() {
        int flag = this.selectionKey.interestOps() & OP_WRITE;

        if (flag == 0) {
            this.selectionKey.interestOps(SelectionKey.OP_READ | OP_WRITE);
        }
    }
}