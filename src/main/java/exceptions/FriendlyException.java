package exceptions;

public class FriendlyException extends RuntimeException {

    public FriendlyException(String exception) {
        super(exception);
    }
}
