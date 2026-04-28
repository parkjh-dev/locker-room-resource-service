package com.lockerroom.resourceservice.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Logical bucket name; combined with the principal to scope the counter. */
    String bucket();

    /** Maximum requests permitted within the window. */
    int max() default 10;

    /** Window length in seconds. */
    int windowSeconds() default 60;
}
