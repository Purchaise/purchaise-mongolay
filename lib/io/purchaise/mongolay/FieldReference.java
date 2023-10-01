package io.purchaise.mongolay;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.lang.reflect.Field;

@Data
@ToString
@AllArgsConstructor
public class FieldReference {
    String source;
    String target;

    public <T> Field getSourceField(Class<T> clazz) {
        String source = hasDynamicSource() ? getFormattedSource() : getSource();
        return FieldUtils.getField(clazz, source, true);
    }

    public String getFormattedSource() {
        return source.replace("{", "").replace("}", "");
    }

    public String getFormattedTarget() {
        return target.replace("{", "").replace("}", "");
    }

    public boolean hasDynamicSource() {
        return source.startsWith("{") && source.endsWith("}");
    }

    public boolean hasDynamicTarget() {
        return target.startsWith("{") && target.endsWith("}");
    }

    public <T> String getMongoSource (Class<T> clazz) {
        String source = hasDynamicSource() ? getFormattedSource() : getSource();
        return FieldReference.getMongoClass(source, clazz);
    }

    public Object getValue (Object item) {
        try {
            return FieldUtils.readField(this.getSourceField(item.getClass()), item, true);
        } catch (NullPointerException e) {
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> String getMongoTarget (Class<T> clazz) {
        String target = hasDynamicTarget() ? getFormattedTarget() : getTarget();
        return FieldReference.getMongoClass(target, clazz);
    }

    public <T> Field getTargetField (Class<T> clazz) {
        String target = hasDynamicTarget() ? getFormattedTarget() : getTarget();
        return FieldUtils.getField(clazz, target, true);
    }

    public Object getTargetValue (Object item) {
        try {
            return FieldUtils.readField(getTargetField(item.getClass()), item, true);
        } catch (NullPointerException e) {
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> String getMongoClass (String field, Class<T> clazz) {
        try {
            Field found = FieldUtils.getField(clazz, field, true);
            return FieldReference.getMongoClass(found);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return field;
        }
    }

    public static <T> String getMongoClass (Field field) {
        BsonProperty[] annotation = field.getAnnotationsByType(BsonProperty.class);
        if (annotation.length > 0) {
            return annotation[0].value();
        }
        return field.getName();
    }
}
