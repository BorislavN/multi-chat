package app.server.minimal.exception;

public class ConnectionException extends RuntimeException{
    public ConnectionException(String message) {
        super(message);
    }
}
