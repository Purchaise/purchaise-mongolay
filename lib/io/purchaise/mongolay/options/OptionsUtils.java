package io.purchaise.mongolay.options;

import com.mongodb.client.model.Aggregates;
import io.purchaise.mongolay.MongoRelay;
import io.purchaise.mongolay.RelayDatabase;
import io.purchaise.mongolay.annotations.OptionField;
import io.purchaise.mongolay.annotations.SubClasses;
import io.purchaise.mongolay.options.enums.OptionType;
import io.purchaise.mongolay.utils.AggregationUtils;
import io.purchaise.mongolay.utils.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.BsonNull;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionsUtils {

    /**
     * Retrieves a list of field names from the specified class. If a field is annotated
     * with {@link BsonProperty}, the value of the annotation is returned instead of the field name.
     *
     * @param clazz the class from which to retrieve field names
     * @return a list of field names, where fields annotated with {@link BsonProperty}
     * return the annotation's value instead of the field's original name
     */
    public static List<String> getCurrentClassFields(Class<?> clazz) {
        return FieldUtils.getAllFieldsList(clazz)
            .stream()
            .map(field -> {
                BsonProperty bsonProperty = field.getAnnotation(BsonProperty.class);
                return bsonProperty != null ? bsonProperty.value() : field.getName();
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of all unique field names from a given class and its subclasses,
     * if any are annotated with {@code @SubClasses}.
     *
     * <p>This method first collects the field names of the provided class, then checks if
     * the class is annotated with {@link SubClasses}. If the annotation is present, the method
     * also collects the field names from the specified subclasses and combines them with the
     * original class's field names. Duplicate field names are removed to return a distinct
     * list of field names.</p>
     *
     * @param <T>   the type of the class from which to retrieve the field names
     * @param clazz the class whose field names are to be retrieved
     * @return a list of unique field names from the given class and its annotated subclasses,
     * or just the field names of the given class if no subclasses are present
     * @throws NullPointerException if {@code clazz} is null
     */
    public static <T> List<String> getAllFieldsNames(Class<T> clazz) {
        List<String> fields = OptionsUtils.getCurrentClassFields(clazz);
        SubClasses subClasses = clazz.getDeclaredAnnotation(SubClasses.class);
        if (subClasses == null) {
            return fields;
        }

        Stream<String> subClassFields = Arrays.stream(subClasses.of())
            .filter(Objects::nonNull)
            .flatMap(subClass -> OptionsUtils.getCurrentClassFields(subClass).stream());
        return Stream.concat(fields.stream(), subClassFields)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific option for the provided class and option type from the MongoRelay object.
     * If no option is found, it returns a default empty FieldConfig.
     *
     * @param database the RelayDatabase which contains both the MongoRelay and the source class
     * @param type     the type of option to retrieve (e.g., CALCULATION)
     * @return the option as an Option object or an empty FieldConfig if no option is found
     */
    public static IOption getOptions(RelayDatabase<?> database, OptionType type, Class<?> clazz) {
        return Optional.ofNullable(database.getMongoRelay())
            .map(MongoRelay::getOptions)
            .map(options -> options.get(clazz))
            .map(document -> document.get(type))
            .orElse(new FieldConfig());
    }

    /**
     * Checks if the given database has calculation options enabled for its source class.
     *
     * @param database the database to check for calculation options
     * @return true if calculation options are present and the source class is not assignable from Bson, false otherwise
     */
    public static boolean hasCalculationOptions(RelayDatabase<?> database) {
        return Optional.ofNullable(database.getMongoRelay())
            .map(MongoRelay::getOptions)
            .map(options -> options.get(database.getSourceClass()))
            .map(document -> document.get(OptionType.CALCULATED))
            .isPresent() && !Bson.class.isAssignableFrom(database.getSourceClass());
    }

    public static boolean hasCalculationOptions(RelayDatabase<?> database, String collectionName) {
        return Optional.ofNullable(database.getMongoRelay())
            .map(MongoRelay::getOptions)
            .stream()
            .anyMatch(options -> options.keySet().stream().anyMatch(clazz -> {
                String entityName = ClassUtils.entityName(clazz);
                if (ClassUtils.isDynamicEntity(clazz)) {
                    return collectionName.startsWith(entityName) && options.get(clazz).containsKey(OptionType.CALCULATED);
                }
                return entityName.equals(collectionName) && options.get(clazz).containsKey(OptionType.CALCULATED);
            }));
    }

    /**
     * Retrieves the name of the first field in the given class that is annotated with {@link OptionField}.
     * If no such field is found, it defaults to returning the name of the option type {@link OptionType#CALCULATED}.
     *
     * @param clazz the class to search for fields annotated with {@link OptionField}
     * @return the name of the first field annotated with {@link OptionField}, or "calculated" if none is found
     */
    public static String getOptionField(Class<?> clazz) {
        return FieldUtils.getFieldsListWithAnnotation(clazz, OptionField.class)
            .stream()
            .findFirst()
            .map(Field::getName)
            .orElse(OptionType.CALCULATED.name().toLowerCase());
    }

    /**
     * Retrieves calculation-specific options for the provided class from the MongoRelay object.
     * This method specifically looks for calculation options (OptionType.CALCULATION).
     *
     * @param database the RelayDatabase which contains both the MongoRelay and the source class
     * @return a FieldConfig object containing the calculation options for the class
     */
    public static FieldConfig getCalculationOptions(RelayDatabase<?> database, Class<?> clazz) {
        return (FieldConfig) OptionsUtils.getOptions(database, OptionType.CALCULATED, clazz);
    }

    /**
     * Merges the given aggregation pipeline with calculation options for the provided class.
     *
     * @param database the RelayDatabase which contains both the MongoRelay and the source class
     * @param pipeline the existing aggregation pipeline to which the calculation options will be added
     * @return a new list of Bson objects representing the merged pipeline
     */
    public static List<? extends Bson> mergeCalculationOptions(RelayDatabase<?> database, List<? extends Bson> pipeline) {
        return OptionsUtils.mergeCalculationOptions(database, database.getSourceClass(), pipeline);
    }

    /**
     * Merges the given aggregation pipeline with calculation options for the provided class.
     *
     * @param database the RelayDatabase which contains both the MongoRelay and the source class
     * @param clazz the source class
     * @param pipeline the existing aggregation pipeline to which the calculation options will be added
     * @return a new list of Bson objects representing the merged pipeline
     */
    public static List<Bson> mergeCalculationOptions(RelayDatabase<?> database, Class<?> clazz, List<? extends Bson> pipeline) {
        // Process lookup stages in the pipeline first
        List<Bson> processedPipeline = pipeline
            .stream()
            .map(stage -> stage.toBsonDocument().containsKey("$lookup") ?
                AggregationUtils.handleLookup(database, Document.parse(stage.toBsonDocument().toString())) :
                stage
            )
            .collect(Collectors.toList());

        if (!OptionsUtils.hasCalculationOptions(database, ClassUtils.entityName(clazz))) {
            return processedPipeline;
        }

        // Merge calculation options if available
        FieldConfig options = OptionsUtils.getCalculationOptions(database, clazz);
        Document combined = options.getConfig();
        combined.putAll(options.getHelperConfig());

        // Process the given class options into a pipeline
        List<Bson> optionsPipeline = new OptionsStagesBuilder().processOptions(combined, clazz);
        Document helperConfigProcessed = OptionsUtils.adjustOptionFieldsAndReferences(options.getHelperConfig(), OptionsUtils.getOptionField(clazz), clazz);
        if (!helperConfigProcessed.isEmpty()) {
            optionsPipeline.add(Aggregates.unset(helperConfigProcessed.keySet().toArray(new String[0])));
        }

        // Combine the options pipeline with the processed pipeline with lookups
        return Stream.concat(optionsPipeline.stream(), processedPipeline.stream()).collect(Collectors.toList());
    }

    /**
     * Adjusts field names and references within a document, prefixing fields with the option field if they are not standard class fields.
     *
     * @param document    The input document containing fields and values.
     * @param optionField The prefix to use for non-standard fields.
     * @param clazz       The class representing the standard fields.
     * @return A new Document with adjusted field names and values.
     */
    public static Document adjustOptionFieldsAndReferences(Document document, String optionField, Class<?> clazz) {
        List<String> classFields = OptionsUtils.getAllFieldsNames(clazz);
        return document.entrySet().stream()
            .map(entry -> {
                String key = entry.getKey();
                String adjustedKey = OptionsUtils.adjustOptionFieldName(key, classFields, optionField);
                Object adjustedValue = OptionsUtils.adjustOptionReferencesInValue(entry.getValue(), classFields, optionField);
                return Map.entry(adjustedKey, adjustedValue);
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                Document::new
            ));
    }

    /**
     * Adjusts the field name by adding the option field as a prefix if the field is not a standard class field.
     *
     * @param key         The original field name.
     * @param classFields The list of standard fields for the class.
     * @param optionField The prefix to use for non-standard fields.
     * @return The adjusted field name.
     */
    private static String adjustOptionFieldName(String key, List<String> classFields, String optionField) {
        String rootField = key.split("\\.")[0];
        if (classFields.contains(rootField) || optionField.isEmpty()) {
            return key;
        }
        return String.format("%s.%s", optionField, key);
    }

    /**
     * Recursively adjusts references within values, adding the option field as a prefix if the reference points to a non-standard field.
     *
     * @param value       The value to be adjusted, which could be a Document, List, or String.
     * @param classFields The list of standard fields for the class.
     * @param optionField The prefix to use for non-standard references.
     * @return The adjusted value.
     */
    private static Object adjustOptionReferencesInValue(Object value, List<String> classFields, String optionField) {
        if (value instanceof Document) {
            return ((Document) value).entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> OptionsUtils.adjustOptionReferencesInValue(entry.getValue(), classFields, optionField),
                    (oldValue, newValue) -> oldValue,
                    Document::new
                ));
        } else if (value instanceof List) {
            return ((List<?>) value)
                .stream()
                .map(element -> OptionsUtils.adjustOptionReferencesInValue(element, classFields, optionField))
                .collect(Collectors.toList());
        } else if (value instanceof String && ((String) value).startsWith("$")) {
            String refField = ((String) value).substring(1);
            String rootRefField = refField.split("\\.")[0];
            if (classFields.contains(rootRefField)) {
                return value;
            }
            return String.format("$%s.%s", optionField, refField);
        }
        return Optional.ofNullable(value).orElse(new BsonNull());
    }

}
