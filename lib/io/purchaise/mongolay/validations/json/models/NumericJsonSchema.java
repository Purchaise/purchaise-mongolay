package io.purchaise.mongolay.validations.json.models;

import io.purchaise.mongolay.validations.json.JsonSchemaUtils;
import lombok.*;
import org.bson.BsonInt64;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NumericJsonSchema extends JsonSchema {
    private BsonInt64 minimum;
    private BsonInt64 maximum;

    public NumericJsonSchema minimum(long minimum) {
        this.minimum = new BsonInt64(minimum);
        return this;
    }

    public NumericJsonSchema maximum(long maximum) {
        this.maximum = new BsonInt64(maximum);
        return this;
    }

    @Override
    public NumericJsonSchema handleFieldAnnotations(Field field) {
        if (Objects.isNull(field)) {
            return this;
        }
        if (field.isAnnotationPresent(NotNull.class)) {
            NotNull notNullAnnotation = field.getAnnotation(NotNull.class);
            this.description(notNullAnnotation.message())
                .bsonType(JsonSchemaUtils.getNonNullableBsonType(this.getBsonType()));
        }
        if (field.isAnnotationPresent(Min.class)) {
            Min minAnnotation = field.getAnnotation(Min.class);
            long minimum = minAnnotation.value();
            String message = minAnnotation.message();
            this.minimum(minimum)
                .description(message);
        }
        if (field.isAnnotationPresent(Max.class)) {
            Max minAnnotation = field.getAnnotation(Max.class);
            long maximum = minAnnotation.value();
            String message = minAnnotation.message();
            this.maximum(maximum)
                .description(message);
        }
        return this;
    }
}
