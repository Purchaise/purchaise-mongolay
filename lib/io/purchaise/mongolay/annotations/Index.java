package io.purchaise.mongolay.annotations;

import io.purchaise.mongolay.utils.IndexType;

import java.lang.annotation.*;


/**
 * @author Agon Lohaj
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Index {
    /**
     * "Direction" of the indexing.  Defaults to {@link IndexType#DESC}.
     *
     * @see IndexType
     */
    IndexType type() default IndexType.DESC;

    /**
     * Whether to run this indexing on the background or not
     */
    boolean background() default true;



}
