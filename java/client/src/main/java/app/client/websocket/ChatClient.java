package app.client.websocket;


//TODO: This interface needs to be implemented by both my "plain" socket implementation, and the "proper" implementation using jakarta ee,
// to achieve polymorphism
public interface ChatClient {
    void sendMessage(String message);
    void closeClient();
}