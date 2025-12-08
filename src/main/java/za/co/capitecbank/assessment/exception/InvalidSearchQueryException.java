package za.co.capitecbank.assessment.exception;

public class InvalidSearchQueryException extends RuntimeException {
    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
