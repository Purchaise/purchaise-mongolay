package io.purchaise.mongolay.validations.json.models;

import io.purchaise.mongolay.validations.json.JsonSchemaUtils;
import lombok.*;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ObjectJsonSchema extends JsonSchema {
    private BsonString title;
    private BsonArray required;
    private BsonDocument properties;
    private BsonDocument dependencies;

    public ObjectJsonSchema title(String title) {
        this.title = new BsonString(String.format("%s Object Validation", title));
        return this;
    }

    public ObjectJsonSchema required(List<String> required) {
        if (required.isEmpty()) {
            return this;
        }

        List<BsonString> requiredValues = required
            .stream()
            .map(BsonString::new)
            .collect(Collectors.toList());
        this.required = new BsonArray(requiredValues);
        return this;
    }

    public ObjectJsonSchema properties(BsonDocument properties) {
        this.properties = properties;
        return this;
    }


    public ObjectJsonSchema dependencies(BsonDocument dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    @Override
    public ObjectJsonSchema handleFieldAnnotations(Field field) {
        if (Objects.isNull(field)) {
            return this;
        }
        if (field.isAnnotationPresent(NotNull.class)) {
            NotNull notNullAnnotation = field.getAnnotation(NotNull.class);
            this.description(notNullAnnotation.message())
                .bsonType(JsonSchemaUtils.getNonNullableBsonType(this.getBsonType()));
        }
        return this;
    }
}
