package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines words that should not be split or modified by the wordDelimiterGraph filter.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @TokenFilterDef(
 *   type = "wordDelimiterGraph",
 *   protectedWords = @ProtectedWords(
 *     words = {"iPhone","eBay"},
 *     ignoreCase = false
 *   )
 * )
 * }</pre>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ProtectedWords {
	String[] words() default {};
	boolean ignoreCase() default true;
}