package io.purchaise.mongolay.annotations;


import java.lang.annotation.*;


/**
 * @author Agon Lohaj
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SubClasses {
    /**
     * If specified
     */
    Class<?>[] of() default {};
}
