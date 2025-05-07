package io.purchaise.mongolay.validations.json.models;

import io.purchaise.mongolay.validations.json.JsonSchemaUtils;
import lombok.*;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.lang.reflect.Field;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CollectionJsonSchema extends JsonSchema {
    private BsonDocument items;
    private BsonBoolean uniqueItems;
    private BsonInt32 minItems;
    private BsonInt32 maxItems;

    public CollectionJsonSchema items(BsonDocument items) {
        this.items = items;
        return this;
    }

    public CollectionJsonSchema uniqueItems(boolean uniqueItems) {
        this.uniqueItems = new BsonBoolean(uniqueItems);
        return this;
    }

    public CollectionJsonSchema minItems(int minItems) {
        this.minItems = new BsonInt32(minItems);
        return this;
    }

    public CollectionJsonSchema maxItems(int maxItems) {
        this.maxItems = new BsonInt32(maxItems);
        return this;
    }

    @Override
    public JsonSchema handleFieldAnnotations(Field field) {
        if (Objects.isNull(field)) {
            return this;
        }
        if (field.isAnnotationPresent(NotNull.class)) {
            NotNull notNullAnnotation = field.getAnnotation(NotNull.class);
            this.description(notNullAnnotation.message())
                .bsonType(JsonSchemaUtils.getNonNullableBsonType(this.getBsonType()));
        }
        if (field.isAnnotationPresent(NotEmpty.class)) {
            NotEmpty notEmptyAnnotation = field.getAnnotation(NotEmpty.class);
            this.minItems(1)
                .description(notEmptyAnnotation.message())
                .bsonType(JsonSchemaUtils.getNonNullableBsonType(this.getBsonType()));
        }
        if (field.isAnnotationPresent(Size.class)) {
            Size sizeAnnotation = field.getAnnotation(Size.class);
            int minItems = sizeAnnotation.min();
            int maxItems = sizeAnnotation.max();
            String message = sizeAnnotation.message();
            this.minItems(minItems)
                .maxItems(maxItems)
                .description(message);
        }
        return this;
    }
}
