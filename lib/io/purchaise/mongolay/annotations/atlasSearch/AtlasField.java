package io.purchaise.mongolay.annotations.atlasSearch;

import io.purchaise.mongolay.utils.FieldType;

import java.lang.annotation.*;

/**
 * Declares the search index mapping for a single Java field.
 * Supports all Atlas Search types and properties, including text, numeric,
 * vector, facet, geo, and nested document configurations.
 * Can be applied multiple times on a field via @AtlasFields.
 *
 * @author Fluture Hundozi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Repeatable(AtlasFields.class)
public @interface AtlasField {
	/* — General — */
	/** Atlas Search type (string, autocomplete, token, number, date, etc.) */
	FieldType type() default FieldType.STRING;

	/* — For text fields — */
	/** Index‐time analyzer (string & autocomplete only) */
	String analyzer() default "";
	/** Search‐time analyzer (string only) */
	String searchAnalyzer() default "";
	/** Indexing strategy: freqs, docs, or positions (string only) */
	String indexOptions() default "";
	/** Whether to store the field separately for retrieval (string only), true if omitted */
	boolean store() default true;
	/** Ignore values whose length exceeds this threshold (string only) */
	int ignoreAbove() default -1;
	/** Whether to include or omit field norms (string only) */
	String norms() default ""; // "include" | "omit"

	/* — For token fields — */
	/** Normalizer (token only: lowercase | none) */
	String normalizer() default "";

	/* — For autocomplete fields — */
	/** Tokenization strategy (edgeGram | rightEdgeGram | nGram) */
	String tokenization() default "";
	/** Minimum n-gram length (autocomplete only) */
	int minGrams() default -1;
	/** Maximum n-gram length (autocomplete only) */
	int maxGrams() default -1;
	/** Diacritic folding flag (autocomplete only), true if omitted */
	boolean foldDiacritics() default true;

	/* — For number and numberFacet fields — */
	/** Value representation: int64 or double */
	String representation() default ""; // "int64" | "double"
	/** Whether to index integers (number/numberFacet only), true if omitted */
	boolean indexIntegers() default true;
	/** Whether to index floating-point numbers (number/numberFacet only), true if omitted */
	boolean indexDoubles() default true;

	/* — For vector embeddings — */
	/** Number of dimensions for vector (knnVector only) */
	int dimensions() default -1;
	/** Similarity function: euclidean, cosine, or dotProduct (knnVector only) */
	String similarity() default "";

	/* — For geo fields — */
	/** Whether to index shapes (geo only), false if omitted */
	boolean indexShapes() default false;

	/* — For nested documents — */
	/** Whether Atlas Search recursively indexes all fields and embedded documents, false if omitted */
	boolean dynamic() default false;

	/* — For fields with multiple types — */
	MultiField[] multi() default {};
}
