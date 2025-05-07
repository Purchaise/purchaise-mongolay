package io.purchaise.mongolay.options;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OptionsStagesBuilder {

    /**
     * Creates a list of $set stages for MongoDB in the correct order based on field dependencies.
     * Fields that do not reference other fields are processed first, followed by fields that depend on previously processed fields.
     *
     * @param computations A Document containing field-value pairs, where values represent MongoDB computations.
     * @param clazz        The class type used for key processing.
     * @return A list of BSON $set stages.
     */
    public List<Bson> processOptions(Document computations, Class<?> clazz) {
        if (computations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Bson> stages = new ArrayList<>();
        while (!computations.isEmpty()) {
            Document previousStage = stages.isEmpty() ? new Document() : (Document) stages.get(stages.size() - 1);
            Document currentStage = extractCurrentStage(computations, previousStage);
            stages.add(currentStage);
            computations = excludeProcessedFields(computations, currentStage);
        }
        return createSetStages(stages, clazz);
    }

    /**
     * Converts a list of Documents into BSON $set stages with key processing.
     *
     * @param stages A list of Documents representing each stage.
     * @param clazz  The class type used for key processing.
     * @return A list of BSON $set stages.
     */
    private List<Bson> createSetStages(List<Bson> stages, Class<?> clazz) {
        return stages
            .stream()
            .map(stage -> OptionsUtils.adjustOptionFieldsAndReferences((Document) stage, OptionsUtils.getOptionField(clazz), clazz))
            .map(stage -> new Document("$set", stage))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a field's value can be computed based on its dependencies.
     * Ensures that a field is either independent or relies on fields already computed.
     *
     * @param field           The field being checked.
     * @param value           The value associated with the field, representing a computation.
     * @param allFields       A Document containing all computations.
     * @param processedFields A Document containing already processed fields.
     * @return True if the field can be computed with current dependencies, false otherwise.
     */
    public boolean isComputable(String field, Object value, Document allFields, Document processedFields) {
        if (value instanceof Document && !((Document) value).containsKey("$literal")) {
            return ((Document) value)
                .entrySet()
                .stream()
                .allMatch(entry -> isComputable(field, entry.getValue(), allFields, processedFields));
        } else if (value instanceof List) {
            return ((List<?>) value)
                .stream()
                .allMatch(element -> isComputable(field, element, allFields, processedFields));
        } else if (value instanceof String && ((String) value).startsWith("$")) {
            String referencedField = ((String) value).substring(1).split("\\.")[0];
            return referencedField.equals(field) ||
                !allFields.containsKey(referencedField) ||
                processedFields.containsKey(referencedField);
        }
        return true;
    }

    /**
     * Extracts fields that can be computed in the current stage based on their dependencies.
     *
     * @param remainingFields A Document of fields yet to be processed.
     * @param processedFields A Document of fields already processed.
     * @return A Document containing fields computable in the current stage.
     */
    private Document extractCurrentStage(Document remainingFields, Document processedFields) {
        return remainingFields
            .entrySet()
            .stream()
            .filter(entry -> isComputable(entry.getKey(), entry.getValue(), remainingFields, processedFields))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                Document::new
            ));
    }

    /**
     * Excludes fields that have been processed from the remaining fields.
     *
     * @param remainingFields The original Document of remaining fields.
     * @param processedFields The Document containing fields that have been processed.
     * @return A Document containing fields that still need processing.
     */
    private Document excludeProcessedFields(Document remainingFields, Document processedFields) {
        return remainingFields
            .entrySet()
            .stream()
            .filter(entry -> !processedFields.containsKey(entry.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                Document::new
            ));
    }

}
