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
