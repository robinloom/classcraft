package com.robinloom.classcraft.example;

import com.robinloom.classcraft.annotations.GenerateBuilder;
import org.jspecify.annotations.Nullable;

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
