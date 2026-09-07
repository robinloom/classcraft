package com.robinloom.classcraft.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a companion structured-logging class for the annotated class.
 *
 * The generated class ({@code <Class>Logger}) exposes static
 * {@code trace}/{@code debug}/{@code info}/{@code warn}/{@code error}
 * (String message, T instance) methods that log the instance's fields as
 * structured key-value pairs via SLF4J's fluent API
 * ({@code log.atInfo().addKeyValue(...).log(message)}), reading each field
 * through its getter — no {@code toString()}, no reflection.
 *
 * Requires a public getter for each field ({@code isX()} is used for boolean
 * fields when present). Fields annotated with {@link Ignore} are skipped.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateLogger {

    /**
     * Suffix appended to the class name for the generated logger class.
     */
    String suffix() default "Logger";
}
