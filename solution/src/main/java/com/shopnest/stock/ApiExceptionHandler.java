package com.shopnest.stock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class ApiExceptionHandler {

    /** Already finished, and it is your example: a 4xx from the warehouse (for example 404) is passed on with the same status. */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ProblemDetail> handleClientError(HttpClientErrorException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.getStatusCode(),
                "The warehouse does not accept this request.");
        return ResponseEntity.status(e.getStatusCode()).body(problem);
    }

    // Task 4: the clear answer when the warehouse still fails after all the retries
    @ExceptionHandler(WarehouseUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleUnavailable(WarehouseUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "We cannot check the stock right now. Please try again in a few seconds.");
        problem.setTitle("Stock check unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "3")
                .body(problem);
    }
}
