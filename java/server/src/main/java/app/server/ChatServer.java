package app.server;

//TODO: this interface should be implemented by both the proper and minimal implmentation
// to achieve polymorphism
public interface ChatServer {
    void startServer(String address,int port);
    void forwardMessage(Message message);
    void shutdownServer();
}