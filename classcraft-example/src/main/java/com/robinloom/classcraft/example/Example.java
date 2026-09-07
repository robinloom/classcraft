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

public class Example {

    static void main(String[] args) {
        System.out.println("=== @GenerateBuilder + @GenerateDTO + @GenerateMapper ===\n");

        // Create entity with builder
        User alice = UserBuilder.builder()
            .name("Alice")
            .email("alice@example.com")
            .phone("555-1234")
            .build();

        System.out.println("Entity: " + alice);

        // Convert entity → DTO using auto-generated static mapper
        UserDTO aliceDTO = UserMapper.toUserDTO(alice);
        System.out.println("DTO:    " + aliceDTO);

        // Convert DTO → entity
        User aliceRestored = UserMapper.toUser(aliceDTO);
        System.out.println("Entity again: " + aliceRestored);

        System.out.println("\n=== @GenerateWither ===\n");

        // Create a modified copy without touching the other fields
        User aliceAtWork = UserWither.withEmail(alice, "alice@work.example.com");
        System.out.println("Original: " + alice);
        System.out.println("Copy:     " + aliceAtWork);

        System.out.println("\n=== @GenerateLogger ===\n");

        // Single-line, compact — safe for log pipelines that treat one line as one entry
        System.out.println(UserLogger.line(alice));

        // JWeaver-style multi-line tree — for console/debug output
        System.out.println(UserLogger.tree(alice));

        // Either rendering, delivered anywhere via Consumer<String>: SLF4J, println, a file, ...
        UserLogger.tree("Suspicious update", aliceAtWork, System.out::println);

        // SLF4J convenience sugar, built on line() (single-line, log-pipeline-safe)
        UserLogger.info("User created", alice);

        System.out.println("\n=== Products (Builder + DTO) ===\n");

        Product laptop = ProductBuilder.builder()
            .name("ThinkPad X1")
            .price(1299.99)
            .sku("TP-X1-2024")
            .build();

        System.out.println(laptop);

        System.out.println("\n=== AppConfig: Mutable (no-arg + setters) ===\n");

        AppConfig config = AppConfigBuilder.builder()
            .appName("MyApp")
            .version("1.0.0")
            .environment("production")
            .build();

        System.out.println("Config: " + config);

        // Convert config → DTO using mapper (will use no-arg + setters)
        AppConfigDTO configDTO = AppConfigMapper.toAppConfigDTO(config);
        System.out.println("ConfigDTO: " + configDTO);

        // Convert back (reverse direction also uses no-arg + setters)
        AppConfig restored = AppConfigMapper.toAppConfig(configDTO);
        System.out.println("Restored: " + restored);
    }
}
