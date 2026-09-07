/*
 * Copyright (C) 2026 Robin Kösters
 * mail[at]robinloom[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
