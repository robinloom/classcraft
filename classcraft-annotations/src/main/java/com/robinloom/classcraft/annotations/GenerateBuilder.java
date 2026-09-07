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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a staged builder class for the annotated constructor.
 * <p>
 * The generated class ({@code <Class>Builder}) forces every non-{@link Nullable}
 * parameter to be supplied — in the constructor's declared order — via a chain
 * of single-method stage interfaces, before any {@link Nullable} parameter can
 * be set and {@code build()} called.
 * <p>
 * Only one constructor per class may be annotated, and it must not be private.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateBuilder {

    /**
     * Suffix appended to the class name for the generated builder class.
     * E.g. suffix = "Assembler" generates UserAssembler instead of UserBuilder.
     */
    String suffix() default "Builder";
}
