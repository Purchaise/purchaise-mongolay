package io.purchaise.mongolay.validations.json.models;

import io.purchaise.mongolay.validations.json.JsonSchemaUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.Objects;

@AllArgsConstructor
//@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ObjectIdJsonSchema extends JsonSchema {

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
        return this;
    }
}
