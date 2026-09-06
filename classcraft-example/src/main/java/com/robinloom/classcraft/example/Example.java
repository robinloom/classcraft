package com.robinloom.classcraft.example;

public class Example {

    static void main(String[] args) {
        System.out.println("=== Using @Nullable from JSpecify ===");

        // Type-safe staged builder pattern with required parameter enforcement
        User alice = UserBuilder.builder()
            .name("Alice")              // Required: NameStage → EmailStage
            .email("alice@example.com") // Required: EmailStage → BuildStage
            .phone("555-1234")          // Optional: BuildStage → BuildStage
            .build();

        System.out.println("User: " + alice.getName() + " (" + alice.getEmail() + "), phone: " + alice.getPhone());

        System.out.println("\n=== Using @Nullable from ClassCraft ===");

        // ClassCraft's own @Optional annotation for projects without external nullability frameworks
        Product laptop = ProductBuilder.builder()
            .name("ThinkPad X1")
            .price(1299.99)
            .sku("TP-X1-2024")
            .build();

        System.out.println("Product: " + laptop.getName() + " ($" + laptop.getPrice() + "), SKU: " + (laptop.getSku() != null ? laptop.getSku() : "unassigned"));

        Product generic = ProductBuilder.builder()
            .name("USB Cable")
            .price(9.99)
            .build();

        System.out.println("Product: " + generic.getName() + " ($" + generic.getPrice() + "), SKU: " + (generic.getSku() != null ? generic.getSku() : "unassigned"));

        // Without optional phone parameter
        User bob = UserBuilder.builder()
            .name("Bob")
            .email("bob@example.com")
            .build();

        System.out.println("User: " + bob.getName() + " (" + bob.getEmail() + "), phone: " + (bob.getPhone() != null ? bob.getPhone() : "n/a"));
    }
}
