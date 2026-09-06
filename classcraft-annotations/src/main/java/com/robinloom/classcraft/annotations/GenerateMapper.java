package com.robinloom.classcraft.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Generates bidirectional mapper (interface + implementation) for converting
 * between the annotated class (typically Entity) and a target class (typically DTO).
 *
 * Generated mapper interface includes:
 * - toTarget(entity) → Target
 * - toEntity(target) → Entity
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateMapper {
    /**
     * Fully qualified class name of target class (typically DTO).
     * Example: "com.example.UserDTO"
     */
    String to();
}
