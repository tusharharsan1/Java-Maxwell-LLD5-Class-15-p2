package com.shopnest.stock;

import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final WarehouseClient warehouseClient;

    public StockService(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
    }

    public StockLevel stockOf(String sku) {
        // TODO 3: when the warehouse still answers with a server error (HttpServerErrorException) after all the
        //         retries, throw new WarehouseUnavailableException(e), so that the caller gets a clear answer.
        //         Every other exception (for example a 404) must go through unchanged.
        return new StockLevel(sku, warehouseClient.availableQuantity(sku));
    }
}
