package com.shopnest.stock;

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

    // TODO 4: add a handler for WarehouseUnavailableException, in the same style as the one above. It must answer:
    //   - HTTP status 503 (SERVICE_UNAVAILABLE)
    //   - a ProblemDetail with the title "Stock check unavailable" and a short detail that a person can read
    //   - the header  Retry-After: 3   (come back in 3 seconds)
}
