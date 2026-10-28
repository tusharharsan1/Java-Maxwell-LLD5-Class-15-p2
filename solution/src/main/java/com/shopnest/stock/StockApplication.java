package com.shopnest.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

// Task 1: this one annotation turns on Spring's retry feature for the whole application.
// Without it, @Retryable (Task 2) is only a comment and does nothing.
@SpringBootApplication
@EnableResilientMethods
public class StockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }
}
