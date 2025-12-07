//package za.co.capitecbank.assessment.exception.handler;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import za.co.capitecbank.assessment.exception.CustomerNotFoundException;
//import za.co.capitecbank.assessment.exception.TransactionNotFoundException;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(TransactionNotFoundException.class)
//    public ResponseEntity<Map<String, String>> handleTransactionNotFound(TransactionNotFoundException ex) {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
//    }
//    @ExceptionHandler(CustomerNotFoundException.class)
//    public ResponseEntity<Map<String, String>> handleCustomerNotFound(CustomerNotFoundException ex) {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
//    }
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "Something went wrong");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//    }
//}