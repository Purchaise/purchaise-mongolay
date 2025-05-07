package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a custom analyzer definition to be included at the index level.
 * Each analyzer must be uniquely named and may be referenced by fields.
 * <p>
 * <strong>Example usage:</strong>
 * <pre>{@code
 * @AnalyzerDef(
 *     name = "myEdgeAnalyzer",
 *     tokenizer = "edgeGram"
 * )
 * }</pre>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface AnalyzerDef {
	/** Unique analyzer name */
	String name();

	/** Tokenizer type (standard | keyword | whitespace | edgeGram | nGram | …) */
	String tokenizer();

	/** — For edgeGram & nGram tokenizers only — */
	int minGram() default -1;
	int maxGram() default -1;

	/** — For regexCaptureGroup tokenizer only — */
	String pattern() default "";
	int group() default -1;

	/** — For standard, uaxUrlEmail & whitespace tokenizers only — */
	int maxTokenLength() default -1;

	/** Optional char‐filters (htmlStrip, mapping, icuNormalize, persian) */
	CharFilterDef[] charFilters() default {};

	/** Optional token‐filters (trim, lowercase, stopword, nGram, shingle, regex, wordDelimiterGraph, …) */
	TokenFilterDef[] tokenFilters() default {};
}