package io.purchaise.mongolay.annotations.atlasSearch;

import io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers.AnalyzerDef;

import java.lang.annotation.*;

/**
 * Declares the configuration for an Atlas Search index at the class level.
 * Specifies index-wide settings like analyzer, dynamic mappings, custom analyzers,
 * synonym maps, stored source projection, and partitioning.
 *
 * @author Fluture Hundozi
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AtlasIndex {
	/**
	 * The name of the search index.
	 */
	String name() default "";

	/**
	 * The default analyzer to apply at index time for string fields.
	 * This is overridden by field-level settings where specified.
	 */
	String analyzer() default "";

	/**
	 * The default analyzer to apply at query time for string fields.
	 * This is used when no field-level searchAnalyzer is defined.
	 */
	String searchAnalyzer() default "";

	/**
	 * Controls whether the mapping should dynamically include all fields
	 * not explicitly defined in the `fields` section. Applies to the root document.
	 */
	boolean dynamic() default true;

	/**
	 * Number of partitions to use for the index.
	 * Improves indexing/querying scalability for large datasets.
	 * If less than or equal to 0, this is ignored.
	 */
	int numPartitions() default -1;

	/**
	 * Defines custom analyzers used by fields in this index.
	 * You must reference these by name in field-level or multi-field definitions.
	 */
	AnalyzerDef[] analyzers() default {};

	/**
	 * Configuration for which fields to store in the index's source metadata.
	 * This enables projection or highlights at query time.
	 */
	StoredSource storedSource() default @StoredSource;

	/**
	 * Synonym mappings to be included in this index.
	 * These can be referenced by synonym-aware analyzers or in queries.
	 */
	Synonym[] synonyms() default {};
}
