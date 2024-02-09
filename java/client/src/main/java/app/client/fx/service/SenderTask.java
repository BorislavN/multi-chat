//package app.client.fx.service;
//
//import app.multicast.client.MulticastClient;
//import javafx.concurrent.Task;
//
//public class SenderTask extends Task<Void> {
////    private final MulticastClient client;
////    private final String message;
////
////    public SenderTask(MulticastClient client, String message) {
////        this.client = client;
////        this.message = message;
////    }
////
////    @Override
////    protected Void call() throws IllegalArgumentException {
////        if (this.message == null) {
////            throw new IllegalArgumentException("Message cannot be null!");
////        }
////
////        if (!this.isCancelled()) {
//////            System.out.println(Thread.currentThread().getName() + " - " + this.message);
////            this.client.sendMessage(this.message);
////        }
////
////        return null;
////    }
//}