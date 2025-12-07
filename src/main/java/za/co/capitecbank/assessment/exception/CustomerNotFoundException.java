package za.co.capitecbank.assessment.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
        super("No record found for customer " + customerId);
    }
}
