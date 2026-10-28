package com.shopnest.stock;

/** The warehouse could not help, even after the retries. Already finished: do not change this file. */
public class WarehouseUnavailableException extends RuntimeException {

    public WarehouseUnavailableException(Throwable cause) {
        super("The warehouse is not answering", cause);
    }
}
