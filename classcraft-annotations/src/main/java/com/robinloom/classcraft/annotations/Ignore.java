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
 * Marks a field to be skipped by ClassCraft's field-based generators
 * ({@link GenerateDTO}, {@link GenerateWither}, {@link GenerateMapper}).
 * <p>
 * For {@code @GenerateMapper}, both classes are still assumed to have
 * identical non-ignored fields in the same order — the same assumption
 * the mapper already makes for the unfiltered case.
 * <p>
 * Not applicable to {@link GenerateBuilder}: a constructor parameter is
 * required to build the object, so it cannot simply be skipped.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Ignore {
}
