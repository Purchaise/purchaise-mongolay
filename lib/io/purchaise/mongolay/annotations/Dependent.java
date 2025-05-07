package io.purchaise.mongolay.annotations;

import io.purchaise.mongolay.validations.json.enums.DependentOperator;

import java.lang.annotation.*;

/**
 * @author Osmon Ahmagjekaj
 *
 * Indicates that a field is dependent on other fields to exist
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependent {
    /**
     * The array of fields the annotated field is dependent on
     */
    String[] on() default {};

    /**
     * Logical operator the dependent fields are connected with
     */
    DependentOperator operator() default DependentOperator.AND;
}
