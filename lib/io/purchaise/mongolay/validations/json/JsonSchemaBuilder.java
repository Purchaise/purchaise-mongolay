package io.purchaise.mongolay.validations.json;

import io.purchaise.mongolay.annotations.Dependent;
import io.purchaise.mongolay.validations.json.enums.DependentOperator;
import io.purchaise.mongolay.validations.json.models.*;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class JsonSchemaBuilder {

    /**
     * Build a json validation schema for the given class
     *
     * @param clazz class type
     * @return BsonDocument
     */
    public BsonDocument build(Class<?> clazz) {
        try {
            if (Objects.isNull(clazz)) {
                throw new IllegalArgumentException("Invalid parameter, class type is null!");
            }
            return new BsonDocument("$jsonSchema", this.getJsonSchema(null, clazz).buildSchema());
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new CompletionException(exception);
        }
    }

    /**
     * Create a JsonSchema object containing the properties for the given field and type
     *
     * @param field to retrieve properties from
     * @param type  used on recursive calls
     * @return JsonSchema object
     */
    private JsonSchema getJsonSchema(Field field, Type type) {
        try {
            if (type instanceof Class) {
                return this.getJsonSchemaFor(field, type);
            } else if (type instanceof ParameterizedType) {
                return this.getJsonSchemaFor(field, (ParameterizedType) type);
            }

            // if the field type is not supported
            throw new UnsupportedOperationException(String.format("Field of type %s not supported!", type));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Create a JsonSchema object containing the properties for the given field and type
     *
     * @param field             to retrieve properties from
     * @param parameterizedType used on recursive calls
     * @return JsonSchema object
     */
    private JsonSchema getJsonSchemaFor(Field field, ParameterizedType parameterizedType) {
        try {
            JsonSchema jsonSchema;
            Class<?> parameterizedClass = (Class<?>) parameterizedType.getRawType();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (Map.class.isAssignableFrom(parameterizedClass)) {
                Type type = typeArguments[1];
                jsonSchema = new MapJsonSchema()
                    .additionalProperties(getJsonSchema(field, type).buildSchema());
            } else if (Collection.class.isAssignableFrom(parameterizedClass)) {
                Type type = typeArguments[0];
                boolean isSet = Set.class.isAssignableFrom(parameterizedClass);
                jsonSchema = new CollectionJsonSchema()
                    .items(getJsonSchema(field, type).buildSchema())
                    .uniqueItems(isSet);
            } else {
                jsonSchema = new ObjectJsonSchema();
                BsonDocument props = new BsonDocument();
                for (Type typeArgument : typeArguments) {
                    props.putAll(getJsonSchema(field, typeArgument).buildSchema());
                }
                ((ObjectJsonSchema) jsonSchema).properties(props);
            }

            BsonString bsonType = JsonSchemaUtils.getBsonType(parameterizedClass);
            jsonSchema.bsonType(JsonSchemaUtils.getNullableBsonType(bsonType));
            jsonSchema.handleFieldAnnotations(field);
            return jsonSchema;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Create a JsonSchema object containing the properties for the given field and type
     *
     * @param field to retrieve properties from
     * @param type  used on recursive calls
     * @return JsonSchema object
     */
    private JsonSchema getJsonSchemaFor(Field field, Type type) {
        try {
            Class<?> clazz = (Class<?>) type;
            BsonString bsonType = JsonSchemaUtils.getBsonType(clazz);
            // if field is of type class, build a class schema
            if (bsonType.equals(BsonType.OBJECT)) {
                return getJsonSchemaFor(field, clazz);
            }

            JsonSchema jsonSchema;
            if (JsonSchemaUtils.isNumeric(bsonType)) {
                jsonSchema = new NumericJsonSchema();
            } else if (bsonType.equals(BsonType.OBJECT_ID)) {
                jsonSchema = new ObjectIdJsonSchema();
            } else if (bsonType.equals(BsonType.STRING)) {
                jsonSchema = new StringJsonSchema();
            } else {
                jsonSchema = new ObjectJsonSchema();
            }

            jsonSchema.bsonType(JsonSchemaUtils.getNullableBsonType(bsonType));
            return jsonSchema.handleFieldAnnotations(field);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Create a JsonSchema object for the given class type
     *
     * @param clazz class type
     * @return JsonSchema object
     */
    private JsonSchema getJsonSchemaFor(Field field, Class<?> clazz) {
        try {
            if (Objects.isNull(clazz)) {
                throw new IllegalArgumentException("Invalid parameter, class type is null!");
            }

            List<Field> fields = new ArrayList<>();
            Class<?> currentClazz = clazz;
            while (Objects.nonNull(currentClazz)) {
                fields.addAll(Arrays.stream(currentClazz.getDeclaredFields()).collect(Collectors.toList()));
                currentClazz = currentClazz.getSuperclass();
            }
            fields = fields
                .stream()
                .filter(next -> !next.isAnnotationPresent(BsonIgnore.class))
                .collect(Collectors.toList());

            ObjectJsonSchema jsonSchema = new ObjectJsonSchema();
            jsonSchema.bsonType(JsonSchemaUtils.getNullableBsonType(BsonType.OBJECT));
            return jsonSchema
                .title(clazz.getSimpleName())
                .required(getRequiredFieldsFrom(fields))
                .properties(getPropertiesFrom(fields))
                .dependencies(getDependenciesFrom(fields))
                .handleFieldAnnotations(field);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new CompletionException(exception);
        }
    }

    /**
     * Get the required fields from the given list of fields
     *
     * @param fields fields
     * @return List of required fields
     */
    private List<String> getRequiredFieldsFrom(List<Field> fields) {
        return fields
            .stream()
            .filter(JsonSchemaUtils::isRequired)
            .map(Field::getName)
            .collect(Collectors.toList());
    }

    /**
     * Get the properties from the given list of fields
     *
     * @param fields fields
     * @return Properties document
     */
    private BsonDocument getPropertiesFrom(List<Field> fields) {
        return fields
            .stream()
            .reduce(
                new BsonDocument(),
                (accumulator, field) -> {
                    accumulator.put(field.getName(), getJsonSchema(field, field.getGenericType()).buildSchema());
                    return accumulator;
                },
                (a, b) -> {
                    BsonDocument all = new BsonDocument();
                    all.putAll(a);
                    all.putAll(b);
                    return all;
                }
            );
    }

    /**
     * Get the dependencies from the given list of fields
     *
     * @param fields fields
     * @return Dependencies document
     */
    private BsonDocument getDependenciesFrom(List<Field> fields) {
        return fields
            .stream()
            .filter(field -> field.isAnnotationPresent(Dependent.class))
            .reduce(
                new BsonDocument(),
                (accumulator, field) -> {
                    Dependent dependentAnnotation = field.getAnnotation(Dependent.class);
                    if (dependentAnnotation.operator().equals(DependentOperator.OR)) {
                        List<BsonDocument> requiredValues = Arrays.stream(dependentAnnotation.on())
                            .map(next -> new BsonDocument("required", new BsonArray(List.of(new BsonString(next)))))
                            .collect(Collectors.toList());
                        BsonDocument anyOf = new BsonDocument("anyOf", new BsonArray(requiredValues));
                        accumulator.put(field.getName(), anyOf);
                    } else {
                        List<BsonString> dependenciesValues = Arrays.stream(dependentAnnotation.on())
                            .map(BsonString::new)
                            .collect(Collectors.toList());
                        accumulator.put(field.getName(), new BsonArray(dependenciesValues));
                    }

                    return accumulator;
                },
                (a, b) -> {
                    BsonDocument all = new BsonDocument();
                    all.putAll(a);
                    all.putAll(b);
                    return all;
                }
            );
    }
}
