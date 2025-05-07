package io.purchaise.mongolay.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


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
