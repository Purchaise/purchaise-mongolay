package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a token filter to be applied after tokenization in a custom analyzer.
 * Supports nGram, shingle, regex, length, stopword, phonetic, normalization, stemming, and more.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @AnalyzerDef(
 *   name = "multiFilterAnalyzer",
 *   tokenizer = "standard",
 *   tokenFilters = {
 *     @TokenFilterDef(
 *       type = "nGram",
 *       minGram = 2, maxGram = 5,
 *       termNotInBounds = "omit"
 *     ),
 *     @TokenFilterDef(
 *       type = "stopword",
 *       tokens = {"a","an","the"},
 *       ignoreCase = true
 *     ),
 *     @TokenFilterDef(
 *       type = "snowballStemming",
 *       stemmerName = "English"
 *     )
 *   }
 * )
 * }</pre>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TokenFilterDef {
	/** trim | lowercase | nGram | shingle | regex | wordDelimiterGraph | … **/
	String type();

	/* — For nGram — */
	/** Minimum n-gram length */
	int minGram() default -1;
	/** Maximum n-gram length */
	int maxGram() default -1;
	/** How to handle tokens out of bounds (include|omit) */
	String termNotInBounds() default "";

	/** For stopword only */
	String[] tokens() default {};

	/** For regex only */
	String pattern() default "";
	String replacement() default "";
	String matches() default "";

	/** For shingle only */
	int minShingleSize() default -1;
	int maxShingleSize() default -1;

	/** For wordDelimiterGraph only */
	DelimiterOptions delimiterOptions() default @DelimiterOptions;
	ProtectedWords protectedWords() default @ProtectedWords;

	/* — For daitchMokotoffSoundex & asciiFolding — */
	/** Include or omit the original tokens (include|omit) */
	String originalTokens() default "";

	/* — For icuNormalizer — */
	/** Normalization form to apply (e.g. nfkc, nfd) */
	String normalizationForm() default "";

	/* — For length — */
	/** Minimum token length */
	int min() default -1;
	/** Maximum token length */
	int max() default -1;

	/* — For snowballStemming — */
	/** Stemmer language name (e.g. English, Porter) */
	String stemmerName() default "";

	/** Whether to ignore case (default true) */
	boolean ignoreCase() default true;
}