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
package com.robinloom.classcraft.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Generates a companion class ({@code <Class>DTO}) that copies the annotated
 * class's fields into a plain class with getters (and setters, unless
 * {@code mutable = false}), an all-args constructor, and optionally
 * {@code equals()}/{@code hashCode()}/{@code toString()} and a no-arg
 * constructor.
 * <p>
 * Fields annotated with {@link Ignore} are skipped.
 */
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
     * <p>
     * Ignored when mutable = false: final fields cannot be left
     * unassigned, so only the all-args constructor is generated.
     */
    boolean generateNoArgConstructor() default true;

    /**
     * Whether the generated class has setters and non-final fields.
     * Set to false for an immutable DTO: fields become final, no
     * setters and no no-arg constructor are generated — only the
     * all-args constructor and getters.
     */
    boolean mutable() default true;

    /**
     * Suffix appended to the class name for the generated DTO class.
     * E.g. suffix = "TO" generates UserTO instead of UserDTO.
     */
    String suffix() default "DTO";
}
