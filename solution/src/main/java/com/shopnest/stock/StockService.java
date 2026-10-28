package com.shopnest.stock;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class StockService {

    private final WarehouseClient warehouseClient;

    public StockService(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
    }

    public StockLevel stockOf(String sku) {
        // Task 3: after all the retries, the last HttpServerErrorException comes out of WarehouseClient.
        // We turn it into our own clear exception. Any other exception (for example a 404) goes through unchanged.
        try {
            return new StockLevel(sku, warehouseClient.availableQuantity(sku));
        } catch (HttpServerErrorException e) {
            throw new WarehouseUnavailableException(e);
        }
    }
}
