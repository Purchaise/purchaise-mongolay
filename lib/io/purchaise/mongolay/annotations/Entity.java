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
     * Sets the collection name to for this entity.  Defaults to the class's simple name
     *
     * @see Class#getSimpleName()
     */
    String collection() default "";
}

