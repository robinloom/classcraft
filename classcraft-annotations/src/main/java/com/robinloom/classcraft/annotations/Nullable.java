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
 *
 * A {@code TYPE_USE} marker rather than a full nullability framework — for
 * interop, {@code org.jspecify.annotations.Nullable} and equivalents from
 * other common frameworks (JSR-305, JetBrains, Checker Framework) are
 * recognized by {@code BuilderProcessor} for the same purpose.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.SOURCE)
public @interface Nullable {
}
