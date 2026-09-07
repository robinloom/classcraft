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
 * Marks a field as sensitive for {@link GenerateLogger}: {@code line()}/{@code tree()}
 * render this field masked instead of calling its getter, so the real value never
 * ends up in a log. The field itself is still shown (unlike {@link Ignore}) — just
 * with the mask in place of its value.
 * <p>
 * Has no effect on {@link GenerateDTO}, {@link GenerateWither} or {@link GenerateMapper}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Sensitive {

    /**
     * The masked value to render instead of the real field value.
     */
    String mask() default "***";
}
