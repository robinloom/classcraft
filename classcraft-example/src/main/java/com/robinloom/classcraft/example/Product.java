package com.robinloom.classcraft.example;

import com.robinloom.classcraft.annotations.GenerateBuilder;
import com.robinloom.classcraft.annotations.Nullable;

public class Product {
    private final String name;
    private final double price;
    private final String sku;

    @GenerateBuilder
    public Product(String name, double price, @Nullable String sku) {
        this.name = name;
        this.price = price;
        this.sku = sku;
    }

    @SuppressWarnings("unused")
    public String getName() { return name; }
    @SuppressWarnings("unused")
    public double getPrice() { return price; }
    @SuppressWarnings("unused")
    public String getSku() { return sku; }
}
