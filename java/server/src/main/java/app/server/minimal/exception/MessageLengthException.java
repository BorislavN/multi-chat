package app.server.minimal.exception;

public class MessageLengthException extends RuntimeException{
    public MessageLengthException(String message) {
        super(message);
    }
}

