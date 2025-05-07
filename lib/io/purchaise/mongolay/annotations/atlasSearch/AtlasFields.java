package io.purchaise.mongolay.annotations.atlasSearch;

import java.lang.annotation.*;

/**
 * Container for repeatable @AtlasField annotations.
 * Allows a single Java field to map to multiple index types or configurations
 * (e.g., string + stringFacet, or autocomplete + exact).
 * <a href="https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html">Click here for more information</a>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AtlasFields {
	AtlasField[] value();
}
