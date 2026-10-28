package com.shopnest.stock;

/**
 * What the shop tells the customer. Already finished: do not change this file.
 *
 * @param sku      the code of the item
 * @param quantity how many pieces the warehouse has
 */
public record StockLevel(String sku, int quantity) {
}
