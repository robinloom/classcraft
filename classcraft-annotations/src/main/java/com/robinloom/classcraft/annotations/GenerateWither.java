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
 * Generates a companion "wither" class with static copy-with-one-field-changed
 * methods for the annotated class.
 * <p>
 * Requires the class to have a constructor whose parameters match all fields
 * by name, and a public getter for each field.
 * <p>
 * Example: for a class {@code User} with field {@code name}, generates
 * {@code UserWither.withName(User source, String name)} returning a new
 * {@code User} with only {@code name} changed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateWither {

    /**
     * Suffix appended to the class name for the generated wither class.
     * E.g. suffix = "Copier" generates UserCopier instead of UserWither.
     */
    String suffix() default "Wither";
}
