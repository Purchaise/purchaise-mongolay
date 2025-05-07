package io.purchaise.mongolay.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a container for repeatable Compound Indexes
 * <a href="https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html">Click here for more information</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CompoundIndexes {
    CompoundIndex[] value();
}