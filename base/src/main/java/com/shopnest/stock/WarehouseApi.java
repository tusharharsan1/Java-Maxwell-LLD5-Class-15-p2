package com.shopnest.stock;

/**
 * The warehouse service: a "friend" that our shop calls. Already finished: do not change this file.
 * In a real shop this would be an HTTP call. Here it is a small interface, so that nothing needs the internet.
 * When the warehouse answers with an error, the call throws the same exceptions that a RestClient throws:
 * HttpServerErrorException for a 5xx code (for example 503), HttpClientErrorException for a 4xx code (for example 404).
 */
public interface WarehouseApi {

    /** How many pieces of this item the warehouse has. */
    int quantityOf(String sku);
}
