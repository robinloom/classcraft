package com.robinloom.classcraft.example;

import com.robinloom.classcraft.annotations.*;
import org.jspecify.annotations.Nullable;

/**
 * Domain Entity with business logic.
 * @GenerateDTO creates UserDTO with getters/setters/equals/hashCode/toString
 * @GenerateMapper creates UserMapper for Entity ↔ DTO conversion
 * @GenerateWither creates UserWither with copy-with-one-field-changed methods
 * @GenerateLogger creates UserLogger with structured, reflection-free logging methods
 *                  (phone is @Sensitive, so it's masked in UserLogger's output)
 */
@GenerateDTO
@GenerateMapper(to = "com.robinloom.classcraft.example.UserDTO")
@GenerateWither
@GenerateLogger
public class User {
    private final String name;
    private final String email;
    @Sensitive
    private final String phone;

    @GenerateBuilder
    public User(String name, String email, @Nullable String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    @SuppressWarnings("unused")
    public String getName() { return name; }
    @SuppressWarnings("unused")
    public String getEmail() { return email; }
    @SuppressWarnings("unused")
    public String getPhone() { return phone; }
}
