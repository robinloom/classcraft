package com.robinloom.classcraft.example;

import com.robinloom.classcraft.annotations.GenerateBuilder;
import com.robinloom.classcraft.annotations.GenerateDTO;
import com.robinloom.classcraft.annotations.GenerateMapper;
import org.jspecify.annotations.Nullable;

/**
 * Domain Entity with business logic.
 * @GenerateDTO creates UserDTO with getters/setters/equals/hashCode/toString
 * @GenerateMapper creates UserMapper for Entity ↔ DTO conversion
 */
@GenerateDTO
@GenerateMapper(to = "com.robinloom.classcraft.example.UserDTO")
public class User {
    private final String name;
    private final String email;
    private final String phone;

    @GenerateBuilder
    public User(String name, String email, @Nullable String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
