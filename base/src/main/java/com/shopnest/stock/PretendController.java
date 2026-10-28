package com.shopnest.stock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The remote control of the pretend warehouse, for practice:
 *   curl "localhost:8080/pretend/warehouse?fail=100&code=503"   fails every call with HTTP 503
 *   curl "localhost:8080/pretend/warehouse?fail=70&code=503"    fails 7 calls in 10
 *   curl "localhost:8080/pretend/warehouse?fail=0"              makes it healthy again
 * Already finished: do not change this file.
 */
@RestController
public class PretendController {

    private final PretendWarehouse warehouse;

    public PretendController(PretendWarehouse warehouse) {
        this.warehouse = warehouse;
    }

    @GetMapping("/pretend/warehouse")
    public String set(@RequestParam(defaultValue = "0") int fail, @RequestParam(defaultValue = "503") int code) {
        warehouse.set(fail, code);
        return "warehouse is now: fail=" + fail + " %, code=" + code;
    }
}
