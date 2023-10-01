package io.purchaise.mongolay.references;


import io.purchaise.mongolay.MongoRelay;
import io.purchaise.mongolay.utils.ClassUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.reflect.FieldUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Data
@ToString
@AllArgsConstructor
public class InnerReferenceField implements IReference {
    Class<?> clazz;
    Field field;
    Class<?>[] subclasses;

    public Object getValue (Object item) {
        try {
            return FieldUtils.readField(field, item, true);
        } catch (NullPointerException e) {
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
     * @param item
     * @param mongoRelay
     * @return
     */
    @Override
    public <A extends Collection<? super TResult>, TResult> A map(A item, MongoRelay mongoRelay) {
        // reset the depth, since we consider this to be an inner reference
        List<Object> values = item.stream()
            .filter(next -> clazz.isAssignableFrom(next.getClass()))
            .map(this::getValue)
            .filter(Objects::nonNull)
            .reduce(new ArrayList<>(), (accumulator, value) -> {
                if (value instanceof Collection) {
                    Collection<Object> list = (Collection<Object>) value;
                    accumulator.addAll(list);
                    return accumulator;
                }
                accumulator.add(value);
                return accumulator;
            }, (a, b) -> {
                ArrayList<Object> all = new ArrayList<>();
                all.addAll(a);
                all.addAll(b);
                return all;
            });
        if (values.size() == 0) {
            return item;
        }
        // fill them up
        int depth = mongoRelay.getDepth();
        MongoRelay clone = mongoRelay.copy().withDepth(depth - 1);
        clone.map(values, ClassUtils.getTargetClass(field));
        // Try mapping the different subclasses as well
        for (Class<?> clazz : subclasses) {
            List<Object> inner = values.stream()
                .filter(next -> clazz.isAssignableFrom(next.getClass()))
                .collect(Collectors.toList());
            clone.map(inner, clazz);
        }
        return item;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
