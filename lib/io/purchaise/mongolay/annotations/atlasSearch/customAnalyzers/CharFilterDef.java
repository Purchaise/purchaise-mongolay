package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a character filter to be applied before tokenization in a custom analyzer.
 * You can strip HTML, apply mappings, normalize ICU forms, or handle Persian-specific text.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @AnalyzerDef(
 *   name = "customHtmlMapper",
 *   tokenizer = "standard",
 *   charFilters = {
 *     @CharFilterDef(
 *       type = "htmlStrip",
 *       ignoredTags = {"b","i","u"}
 *     ),
 *     @CharFilterDef(
 *       type = "mapping",
 *       mapping = {
 *         @Mapping(key = "@", value = "at"),
 *         @Mapping(key = "#", value = "number")
 *       }
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
public @interface CharFilterDef {
	/** e.g. htmlStrip | mapping */
	String type();
	/** For htmlStrip type only: which tags to ignore */
	String[] ignoredTags() default {};
	/** For mapping type only: key/value pairs, e.g. "@":"AT " */
	Mapping[] mapping() default {};
}