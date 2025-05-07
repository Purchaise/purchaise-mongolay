package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;

/**
 * Indicates that this field will be used as the container object for the calculated options fields.
 *
 * @author Osmon Ahmagjekaj
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionField {
}
