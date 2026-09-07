package com.robinloom.classcraft.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Generates a companion "wither" class with static copy-with-one-field-changed
 * methods for the annotated class.
 *
 * Requires the class to have a constructor whose parameters match all fields
 * by name, and a public getter for each field.
 *
 * Example: for a class {@code User} with field {@code name}, generates
 * {@code UserWither.withName(User source, String name)} returning a new
 * {@code User} with only {@code name} changed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateWither {
}
