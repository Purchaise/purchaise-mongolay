package io.purchaise.mongolay.annotations.atlasSearch;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures how the index stores source fields for query-time projections.
 * Can enable or disable stored source, and optionally include or exclude specific fields.
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface StoredSource {
	/** If true and no includes/excludes, storedSource: true */
	boolean enabled() default false;
	/** List of field names to include */
	String[] include() default {};
	/** List of field names to exclude */
	String[] exclude() default {};
}