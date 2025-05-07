package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;


/**
 * @author Agon Lohaj
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface VectorSearchIndex {
    /**
     * Filters to use combined with this index, optional, useful for Vector Search
     * @return
     */
    String[] filters() default {};

    /**
     * Vector Search only configuration
     * @return
     */
    int numOfDimensions() default 1536;

    /**
     * Field to use for Compound Indexes
     */
    String field() default "";

    /**
     * The similarity to use to index the field
     * @return
     */
    String similarity() default "cosine";

}
