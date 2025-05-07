package io.purchaise.mongolay.annotations;


import io.goprime.mongolay.Constants;

import java.lang.annotation.*;


/**
 * @author Agon Lohaj
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {
    /**
     * The name of the Mongo value to store the field. Defaults to the name of the field being annotated.
     */
    String[] from() default { Constants.ID_KEY };

    /**
     * The name of the Mongo value to store the field. Defaults to the name of the field being annotated.
     */
    String[] to() default { Constants.ID_KEY };

    /**
     * If during mapping we want to use projections or not
     */
    String projections() default "";

    /**
     * If specified
     */
    Class<?>[] subclasses() default {};

    /**
     * Whether this class has a nested reference or not
     */
    boolean nested() default false;

    /**
     * Whether this class doesn't contain @Entity annotation
     */
    String collection() default "";
}
