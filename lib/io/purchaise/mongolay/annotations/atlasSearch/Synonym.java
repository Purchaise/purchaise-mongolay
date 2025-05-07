package io.purchaise.mongolay.annotations.atlasSearch;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a synonym mapping definition for the index.
 * Synonyms can be loaded from a source collection and applied via analyzers.
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Synonym {
	/** Unique mapping name */
	String name();
	/** Source collection for synonyms */
	String sourceCollection();
	/** Analyzer to apply for synonym lookups */
	String analyzer() default "";
}