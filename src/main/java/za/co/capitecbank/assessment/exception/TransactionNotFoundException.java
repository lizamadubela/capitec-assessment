package za.co.capitecbank.assessment.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id, String customerId) {
        super("Transaction " + id + " was not found for this customer id: " + customerId);
    }
}
