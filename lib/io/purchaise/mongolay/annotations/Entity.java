package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;

/**
 * @author Agon Lohaj
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Entity {
    /**
     * Sets the collection name to for this entity. Defaults to the class's simple name
     *
     * @see Class#getSimpleName()
     */
    String collection() default "";

    /**
     * Indicates whether the entity is dynamic, meaning it can have multiple collections
     * that vary based on runtime conditions (e.g, postfixes or other criteria).
     */
    boolean dynamic() default false;
}
