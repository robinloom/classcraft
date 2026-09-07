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
 * Marks a type use as nullable. Recognized by {@link GenerateBuilder}: a
 * constructor parameter annotated {@code @Nullable} (and not a primitive) is
 * treated as optional — settable after all required parameters — rather than
 * forced into the required staged chain.
 * <p>
 * A {@code TYPE_USE} marker rather than a full nullability framework — for
 * interop, {@code org.jspecify.annotations.Nullable} and equivalents from
 * other common frameworks (JSR-305, JetBrains, Checker Framework) are
 * recognized by {@code BuilderProcessor} for the same purpose.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.SOURCE)
public @interface Nullable {
}
