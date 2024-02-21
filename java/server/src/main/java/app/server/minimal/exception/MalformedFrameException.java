package app.server.minimal.exception;

public class MalformedFrameException extends RuntimeException{
    public MalformedFrameException(String message) {
        super(message);
    }
}
