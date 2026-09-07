package com.robinloom.classcraft.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be skipped by ClassCraft's field-based generators
 * ({@link GenerateDTO}, {@link GenerateWither}, {@link GenerateMapper}).
 *
 * For {@code @GenerateMapper}, both classes are still assumed to have
 * identical non-ignored fields in the same order — the same assumption
 * the mapper already makes for the unfiltered case.
 *
 * Not applicable to {@link GenerateBuilder}: a constructor parameter is
 * required to build the object, so it cannot simply be skipped.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Ignore {
}
