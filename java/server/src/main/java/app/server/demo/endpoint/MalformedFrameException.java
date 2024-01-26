package app.server.demo.endpoint;

public class MalformedFrameException extends RuntimeException{
    public MalformedFrameException(String message) {
        super(message);
    }
}
