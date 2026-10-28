package com.shopnest.stock;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * A pretend warehouse, for practice. It always says 7 pieces, unless you make it fail
 * (see PretendController). Each call writes one line in the console, so you can count
 * the calls that really reach the warehouse.
 * Already finished: do not change this file.
 */
@Component
public class PretendWarehouse implements WarehouseApi {

    private volatile int failPercent = 0;   // how often the warehouse fails
    private volatile int failCode = 503;    // the HTTP code it fails with

    public void set(int failPercent, int failCode) {
        this.failPercent = failPercent;
        this.failCode = failCode;
    }

    @Override
    public int quantityOf(String sku) {
        boolean fail = ThreadLocalRandom.current().nextInt(100) < failPercent;
        System.out.println("pretend warehouse: " + sku + " -> " + (fail ? failCode : "7 pieces"));
        if (fail) {
            HttpStatusCode status = HttpStatusCode.valueOf(failCode);
            if (failCode >= 500) {
                throw new HttpServerErrorException(status);
            }
            throw new HttpClientErrorException(status);
        }
        return 7;
    }
}
