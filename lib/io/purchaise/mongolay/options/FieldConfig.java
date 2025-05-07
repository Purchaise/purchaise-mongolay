package io.purchaise.mongolay.options;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

/**
 * A configuration class for managing field calculations in MongoDB projections.
 * This class contains main and helper calculation configs.
 *
 * <p>This class is commonly used in conjunction with MongoDB operations that require
 * custom calculations or projections based on field configurations.</p>
 *
 * @see IOption
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FieldConfig implements IOption {

    /**
     * The main configuration document holding various field-related settings.
     * This document typically contains information about how fields should be processed
     * or calculated in MongoDB queries/aggregations.
     */
    private Document config = new Document();

    /**
     * The helper configuration document, which is used to store additional
     * metadata or helper information that can be referenced during processing.
     */
    private Document helperConfig = new Document();
}
