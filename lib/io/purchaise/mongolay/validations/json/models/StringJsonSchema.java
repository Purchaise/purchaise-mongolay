package io.purchaise.mongolay.validations.json.models;

import io.purchaise.mongolay.validations.json.JsonSchemaUtils;
import lombok.*;
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
public class StringJsonSchema extends JsonSchema {
    private BsonInt32 minLength;
    private BsonInt32 maxLength;

    public StringJsonSchema minLength(int minLength) {
        this.minLength = new BsonInt32(minLength);
        return this;
    }

    public StringJsonSchema maxLength(int maxLength) {
        this.maxLength = new BsonInt32(maxLength);
        return this;
    }

    @Override
    public StringJsonSchema handleFieldAnnotations(Field field) {
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
            this.minLength(1)
                .description(notEmptyAnnotation.message())
                .bsonType(JsonSchemaUtils.getNonNullableBsonType(this.getBsonType()));
        }
        if (field.isAnnotationPresent(Size.class)) {
            Size sizeAnnotation = field.getAnnotation(Size.class);
            int minLength = sizeAnnotation.min();
            int maxLength = sizeAnnotation.max();
            String message = sizeAnnotation.message();
            this.minLength(minLength)
                .maxLength(maxLength)
                .description(message);
        }
        return this;
    }
}
