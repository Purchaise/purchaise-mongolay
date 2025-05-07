package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;

/**
 * The annotated element must exist. Accepts any type, including null.
 *
 * @author Osmon Ahmagjekaj
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
}
