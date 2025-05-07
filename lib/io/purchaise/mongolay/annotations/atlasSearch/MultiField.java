package io.purchaise.mongolay.annotations.atlasSearch;


import io.purchaise.mongolay.utils.FieldType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a sub-field mapping inside the "multi" section of a string field.
 * Allows the same logical field to be indexed under multiple sub-names
 * with different types or analyzers (e.g., for autocomplete or exact match).
 *
 * @author Fluture Hundozi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface MultiField {
	/** Subfield name inside `multi` */
	String name() default "";

	/** Subfield type (string, autocomplete, etc.) */
	FieldType type() default FieldType.STRING;

	/** Optional properties per subfield type */
	String analyzer() default "";
	String searchAnalyzer() default "";
	String tokenization() default "";
	int minGrams() default -1;
	int maxGrams() default -1;
	boolean foldDiacritics() default true;
}
