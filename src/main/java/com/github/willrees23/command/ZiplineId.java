package com.github.willrees23.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter that names an existing zipline, so that it completes with the ids currently on
 * the server.
 *
 * <p>The completions hang off this annotation rather than off {@link String} itself, which would
 * offer zipline ids for every piece of text any command takes. The provider behind it is registered
 * in {@code ZiplinesPlugin}, where the manager holding the ids is to hand.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZiplineId {
}
