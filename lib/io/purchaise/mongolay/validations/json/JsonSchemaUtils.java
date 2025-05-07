package io.purchaise.mongolay.validations.json;

import io.purchaise.mongolay.annotations.Dependent;
import io.purchaise.mongolay.annotations.Required;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

import javax.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JsonSchemaUtils {

    private static final List<Class<? extends Annotation>> ANNOTATIONS = List.of(
        NotEmpty.class,
        Required.class,
        NotNull.class,
        Dependent.class,
        Size.class,
        Min.class,
        Max.class
    );

    /**
     * Indicates whether the field has valid annotations (by valid meaning the ones we check for, check ANNOTATIONS list at the top),
     * that is if the field is a complex type, we check further for its own fields, otherwise we check for the type at hand.
     *
     * @param field to check for annotations
     * @return Boolean indicating if the field has any valid annotations
     */
    public static boolean hasValidAnnotations(Field field) {
        if (field.getDeclaredAnnotations().length == 0) {
            return false;
        }
        return ANNOTATIONS.stream().anyMatch(field::isAnnotationPresent);
    }

    /**
     * Indicates whether the given field is a complex type,
     * that is whether the field is of type object or a parameterized type.
     *
     * @param field to check its type
     * @return Boolean indicating if the field is a complex type
     */
    public static boolean isComplexType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            return true;
        }
        BsonString bsonType = getBsonType((Class<?>) type);
        return bsonType.equals(BsonType.OBJECT) || bsonType.equals(BsonType.ARRAY);
    }

    /**
     * Indicates whether the given field is required,
     * that is whether the field has an annotation of type NotEmpty or NotNull present.
     *
     * @param field to check its annotations
     * @return Boolean indicating if the field is required
     */
    public static boolean isRequired(Field field) {
        return field.isAnnotationPresent(Required.class) || field.isAnnotationPresent(NotEmpty.class);
    }

    public static boolean isNumeric(BsonString bsonType) {
        return List.of(
            BsonType.DECIMAL,
            BsonType.DOUBLE,
            BsonType.INTEGER,
            BsonType.LONG
        ).contains(bsonType);
    }

    /**
     * Maps bsonType of type BsonString to a BsonArray used on the jsonSchema to allow multiple data types for a field,
     * including null values
     *
     * @param bsonType BsonString type
     * @return BsonArray consisting of the available data types for a field
     */
    public static BsonArray getNullableBsonType(BsonString bsonType) {
        BsonArray bsonArray = getNonNullableBsonType(bsonType);
        bsonArray.add(BsonType.NULL);
        return bsonArray;
    }

    /**
     * Maps bsonType of type BsonString to a BsonArray used on the jsonSchema to allow multiple data types for a field,
     * not including null values
     *
     * @param bsonType BsonString type
     * @return BsonArray consisting of the available non-nullable data types for a field
     */
    public static BsonArray getNonNullableBsonType(BsonString bsonType) {
        if (bsonType.equals(BsonType.DOUBLE)) {
            return new BsonArray(List.of(BsonType.INTEGER, BsonType.DOUBLE));
        }
        return new BsonArray(List.of(bsonType));
    }

    /**
     * Filter out the null value from the given bsonType
     *
     * @param bsonType BsonArray type
     * @return BsonArray consisting of the available non-nullable data types for a field
     */
    public static BsonArray getNonNullableBsonType(BsonArray bsonType) {
        if (Objects.isNull(bsonType)) {
            return null;
        }
        List<BsonString> nonNullableBsonType = bsonType
            .stream()
            .map(BsonValue::asString)
            .filter(next -> !next.getValue().equals(BsonType.NULL.getValue()))
            .collect(Collectors.toList());
        return new BsonArray(nonNullableBsonType);
    }

    /**
     * Map Java type to Bson type dynamically.
     *
     * @param javaType java type to be mapped
     * @return BsonString representing the bsonType
     */
    public static BsonString getBsonType(Class<?> javaType) {
        if (javaType.isAssignableFrom(ObjectId.class)) {
            return BsonType.OBJECT_ID;
        }
        if (javaType == Boolean.class || javaType == boolean.class) {
            return BsonType.BOOLEAN;
        }
        if (javaType == Double.class || javaType == double.class) {
            return BsonType.DOUBLE;
        }
        if (javaType == Float.class || javaType == float.class) {
            return BsonType.DOUBLE;
        }
        if (javaType == Integer.class || javaType == int.class) {
            return BsonType.INTEGER;
        }
        if (javaType == Long.class || javaType == long.class) {
            return BsonType.LONG;
        }
        if (javaType == String.class || javaType.isEnum()) {
            return BsonType.STRING;
        }
        if (Collection.class.isAssignableFrom(javaType)) {
            return BsonType.ARRAY;
        }
        return BsonType.OBJECT;
    }
}
