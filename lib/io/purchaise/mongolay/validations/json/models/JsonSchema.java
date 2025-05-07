package io.purchaise.mongolay.validations.json.models;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode
public abstract class JsonSchema {
    private BsonArray bsonType;
    private BsonString description;

    public JsonSchema bsonType(BsonArray bsonType) {
        this.bsonType = bsonType;
        return this;
    }

    public JsonSchema bsonType(List<BsonString> bsonType) {
        return this.bsonType(new BsonArray(bsonType));
    }

    public JsonSchema bsonType(BsonString... bsonType) {
        return this.bsonType(List.of(bsonType));
    }

    public JsonSchema bsonType(BsonString bsonType) {
        return this.bsonType(List.of(bsonType));
    }

    public JsonSchema description(String description) {
        String value = description;
        if (Objects.nonNull(this.description)) {
            value = String.format("%s, %s", this.description.getValue(), description);
        }
        this.description = new BsonString(value);
        return this;
    }

    public abstract JsonSchema handleFieldAnnotations(Field field);

    /**
     * Build the json validation schema for the given properties
     *
     * @return BsonDocument representing the json schema
     */
    public BsonDocument buildSchema() {
        BsonDocument schema = new BsonDocument();
        Class<?> currentClazz = this.getClass();
        if (Objects.isNull(currentClazz)) {
            return schema;
        }

        List<Field> fields = new ArrayList<>();
        while (Objects.nonNull(currentClazz)) {
            fields.addAll(List.of(currentClazz.getDeclaredFields()));
            currentClazz = currentClazz.getSuperclass();
        }

        fields.forEach(field -> {
            try {
                String fieldName = field.getName();
                String getterMethodName = String.format("get%s", StringUtils.capitalize(fieldName));
                Method getterMethod = this.getClass().getMethod(getterMethodName);
                BsonValue fieldValue = (BsonValue) getterMethod.invoke(this);
                if (fieldValue != null) {
                    schema.put(fieldName, fieldValue);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        return schema;
    }
}
