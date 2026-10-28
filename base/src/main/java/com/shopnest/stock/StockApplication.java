package com.shopnest.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// TODO 1: turn on Spring's retry feature for the whole application (one annotation on this class).
//         Without it, @Retryable (Task 2) is only a comment and does nothing.
@SpringBootApplication
public class StockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }
}
