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
 *
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
