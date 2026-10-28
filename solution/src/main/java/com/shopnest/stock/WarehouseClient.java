package com.shopnest.stock;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/** The one place in our shop that talks to the warehouse. */
@Component
public class WarehouseClient {

    private final WarehouseApi warehouseApi;

    public WarehouseClient(WarehouseApi warehouseApi) {
        this.warehouseApi = warehouseApi;
    }

    // Task 2: retry only a server error (5xx), 2 retries after the first try (3 calls at most),
    // waits of about 100 ms and then about 300 ms, each changed a little by the jitter.
    @Retryable(includes = HttpServerErrorException.class, maxRetries = 2, delay = 100, multiplier = 3, jitter = 50)
    public int availableQuantity(String sku) {
        return warehouseApi.quantityOf(sku);
    }
}
