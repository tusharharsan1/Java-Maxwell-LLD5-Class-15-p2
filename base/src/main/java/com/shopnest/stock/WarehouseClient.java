package com.shopnest.stock;

import org.springframework.stereotype.Component;

/** The one place in our shop that talks to the warehouse. */
@Component
public class WarehouseClient {

    private final WarehouseApi warehouseApi;

    public WarehouseClient(WarehouseApi warehouseApi) {
        this.warehouseApi = warehouseApi;
    }

    // TODO 2: make this method retry, patiently, with one annotation:
    //   - only when the warehouse answers with a server error (HttpServerErrorException, HTTP 5xx)
    //   - 2 retries after the first try (so at most 3 calls in all)
    //   - the first wait is 100 ms, and each wait is 3 times the one before (100, 300)
    //   - a jitter of 50 ms
    public int availableQuantity(String sku) {
        return warehouseApi.quantityOf(sku);
    }
}
