package app.server.minimal;

public class MalformedFrameException extends RuntimeException{
    public MalformedFrameException(String message) {
        super(message);
    }
}
