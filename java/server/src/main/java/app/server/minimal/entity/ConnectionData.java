package app.server.minimal.entity;

import app.server.minimal.exception.MessageLengthException;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

import static app.util.Constants.MESSAGE_LIMIT;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class ConnectionData {
    private String username;
    private boolean wasUpgraded;
    private boolean receivedClose;
    private boolean sentClose;
    private int totalFragmentLength;
    private FrameData currentFrame;
    private final List<ByteBuffer> fragments;
    private final SelectionKey selectionKey;
    private final Deque<ByteBuffer> waitingFrames;

    public ConnectionData(SelectionKey key) {
        this.selectionKey = key;
        this.username = null;
        this.wasUpgraded = false;
        this.receivedClose = false;
        this.sentClose = false;
        this.totalFragmentLength = 0;
        this.currentFrame = new FrameData();
        this.waitingFrames = new ArrayDeque<>();
        this.fragments = new ArrayList<>();
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

    public boolean receivedClose() {
        return this.receivedClose;
    }

    public void setReceivedClose(boolean receivedClose) {
        this.receivedClose = receivedClose;
    }

    public boolean sentClose() {
        return this.sentClose;
    }

    public void setSentClose(boolean sentClose) {
        this.sentClose = sentClose;
    }

    public SelectionKey getSelectionKey() {
        return this.selectionKey;
    }

    public SocketChannel getConnection() {
        return (SocketChannel) this.selectionKey.channel();
    }

    public FrameData getCurrentFrame() {
        return this.currentFrame;
    }

    public void resetCurrentFrame() {
        this.currentFrame = new FrameData();
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
        this.totalFragmentLength += fragment.capacity();

        if (this.totalFragmentLength > MESSAGE_LIMIT) {
            throw new MessageLengthException("Message limit exceed!");
        }

        this.fragments.add(fragment);
    }

    public void enqueueFragments(List<ByteBuffer> fragments) {
        this.waitingFrames.addAll(fragments);
        this.registerForWrite();
    }

    public void clearFragments() {
        this.totalFragmentLength = 0;
        this.fragments.clear();
    }

    private void registerForWrite() {
        int flag = this.selectionKey.interestOps() & OP_WRITE;

        if (flag == 0) {
            this.selectionKey.interestOps(SelectionKey.OP_READ | OP_WRITE);
        }
    }
}