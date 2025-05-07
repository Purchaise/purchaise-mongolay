package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration options for the Word Delimiter and Graph Filter.
 * Controls splitting, concatenation, and whether to preserve the original token.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @TokenFilterDef(
 *   type = "wordDelimiterGraph",
 *   delimiterOptions = @DelimiterOptions(
 *     generateWordParts = true,
 *     concatenateAll = true,
 *     preserveOriginal = true
 *   )
 * )
 * }</pre>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface DelimiterOptions {
	boolean generateWordParts() default true;
	boolean generateNumberParts() default true;
	boolean concatenateWords() default false;
	boolean concatenateNumbers() default false;
	boolean concatenateAll() default false;
	boolean preserveOriginal() default true;
	boolean splitOnCaseChange() default true;
	boolean splitOnNumerics() default true;
	boolean stemEnglishPossessive() default true;
	boolean ignoreKeywords() default false;
}