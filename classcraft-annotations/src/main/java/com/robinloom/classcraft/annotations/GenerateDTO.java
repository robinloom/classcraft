package com.robinloom.classcraft.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateDTO {
    /**
     * Generate equals() and hashCode() based on all fields.
     */
    boolean generateEqualsHashCode() default true;

    /**
     * Generate toString() based on all fields.
     */
    boolean generateToString() default true;

    /**
     * Generate a no-arg constructor.
     */
    boolean generateNoArgConstructor() default true;
}
