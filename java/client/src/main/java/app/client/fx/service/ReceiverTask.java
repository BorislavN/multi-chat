//package app.client.fx.service;
//
//import app.multicast.client.MulticastClient;
//import javafx.application.Platform;
//import javafx.collections.ObservableList;
//import javafx.concurrent.Task;
//
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//
//public class ReceiverTask extends Task<Void> {
//    private final MulticastClient client;
//    private final ObservableList<String> messageList;
//
//    public ReceiverTask(MulticastClient client, ObservableList<String> messageList) {
//        this.client = client;
//        this.messageList = messageList;
//    }
//
//    @Override
//    protected Void call() throws Exception {
//        String tempGroup = this.client.getGroupIP();
//        int tempPort = this.client.getPort();
//
//        Selector selector = this.client.getSelector();
//
//        this.logMessage(tempGroup, tempPort, "ReceiverTask starting...");
//
//        while (this.client.isLive()) {
//            //The "select" is blocking to save CPU resources, if the thread is interrupted, the selector unblocks
//            selector.select();
//
//            if (this.isCancelled() || !selector.isOpen()) {
//                break;
//            }
//
//            for (SelectionKey key : selector.selectedKeys()) {
//                if (key.isValid() && key.isReadable()) {
//                    String message = this.client.receiveMessage();
//
//                    if (message != null) {
//                        Platform.runLater(() -> this.messageList.add(message));
//                    }
//                }
//            }
//        }
//
//        this.logMessage(tempGroup, tempPort, "ReceiverTask finishing...");
//
//        return null;
//    }
//
//    private void logMessage(String group, int port, String message) {
//        System.out.println("----------------------------------------------");
//        System.out.println(Thread.currentThread().getName() + ":");
//        System.out.printf("%s:%d - %s%n", group, port, message);
//        System.out.println("----------------------------------------------");
//    }
//}