//package app.client.fx.service;
//
//import app.multicast.client.MulticastClient;
//import javafx.concurrent.Service;
//import javafx.concurrent.Task;
//
//import java.util.concurrent.Executors;

//"The Service by default uses a thread pool Executor with some unspecified default or maximum thread pool size.
// This is done so that naive code will not completely swamp the system by creating thousands of Threads." - javafx.concurrent.Service
//public class SenderService extends Service<Void> {
//    private final MulticastClient client;
//    private String currentMessage;
//
//    //The default Executor crates many threads, but they are shortlisted and as the documentation states there is a limit
//    //Alternatively we can set our own executor, in this example it's a CachedThreadPool (reuses the thread if it's free, or creates a new one)
//    //which as the default Executor uses Demon Threads
//    public SenderService(MulticastClient client) {
//        this.client = client;
//        this.currentMessage = null;
//
//        //With this executor, majority of messages reuse the thread
//        //You can see for yourself - uncomment the "println" statement in app.fxchat.multicast.service.SenderTask
//        this.setExecutor(Executors.newCachedThreadPool(r -> {
//            Thread thread = new Thread(r);
//            thread.setDaemon(true);
//
//            return thread;
//        }));
//    }
//
//    public void sendMessage(String username, String message) {
//        this.currentMessage = String.format("%s: %s", username, message);
//        this.executeTask(this.createTask());
//    }
//
//    public void sendMessage(String message) {
//        this.currentMessage = message;
//        this.executeTask(this.createTask());
//    }
//
//    @Override
//    protected Task<Void> createTask() {
//        return new SenderTask(this.client, this.currentMessage);
//    }
//}