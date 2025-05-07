package io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A simple key/value pair used for character mappings in {@link CharFilterDef}.
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @CharFilterDef(
 *   type = "mapping",
 *   mapping = {
 *     @Mapping(key = "æ", value = "ae"),
 *     @Mapping(key = "œ", value = "oe")
 *   }
 * )
 * }</pre>
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Mapping {
	String key();
	String value();
}