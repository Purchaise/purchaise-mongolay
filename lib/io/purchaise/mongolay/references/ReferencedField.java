package io.purchaise.mongolay.references;


import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import io.purchaise.mongolay.FieldReference;
import io.purchaise.mongolay.MongoRelay;
import io.purchaise.mongolay.RelayDatabase;
import io.purchaise.mongolay.RelayFindIterable;
import io.purchaise.mongolay.utils.ClassUtils;
import io.purchaise.mongolay.utils.TextUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Data
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class ReferencedField implements IReference {
    Class<?> clazz;
    Field field;
    String projections;
    List<FieldReference> referencesList = new ArrayList<>();
    String targetCollection;

    public void addReference(FieldReference reference) {
        referencesList.add(reference);
    }

    public boolean isCollectionField () {
        return ClassUtils.isCollectionField(field.getType());
    }

    public boolean isMultipleFieldReference () {
        return referencesList.size() > 1;
    }

    public Class<?> getTargetClass () {
        return ClassUtils.getTargetClass(field);
    }

    /**
     * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
     * @param item
     * @param mongoRelay
     * @return
     */
    @Override
    public <A extends Collection<? super TResult>, TResult> A map(A item, MongoRelay mongoRelay) {
        Collection<Object> items = this.find(item, mongoRelay);
        if (items.size() == 0) {
            return item;
        }
        // The assign part!
        for (Object source: item) {
            if (!clazz.isAssignableFrom(source.getClass())) {
                continue;
            }
            // find the one value that matches at the resulted values
            Collection<Object> found = items.stream()
                    .filter(target -> this.getReferencesList().stream().allMatch((next) -> this.match(source, target, next)))
                    .collect(Collectors.toList());
            try {
                if (this.isCollectionField()) {
                    FieldUtils.writeField(source, this.getField().getName(), found, true);
                } else if (found.size() > 0) {
                    FieldUtils.writeField(source, this.getField().getName(), found.iterator().next(), true);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return item;
    }

    @Override
    public boolean isValid() {
        return referencesList.size() > 0;
    }

    private boolean match (Object source, Object target, FieldReference next) {
        if(next.hasDynamicSource() || next.hasDynamicTarget()) {
            return true;
        }
        Object sourceValue = next.getValue(source);
        if (sourceValue == null) {
            return next.getTargetValue(target) == null;
        }
        Object targetValue = next.getTargetValue(target);
        if (targetValue == null) {
            return true;
        }
        if (sourceValue instanceof Collection) {
            Collection sourceCollection = (Collection) sourceValue;

            if (targetValue instanceof Collection) {
                Collection targetCollection = (Collection) targetValue;
                return targetCollection.stream().anyMatch(sourceCollection::contains);
            }
            // Check if source was a list,
            // then matches if one of the values is within
            return sourceCollection.contains(targetValue);
        }

        if (targetValue instanceof Collection) {
            Collection targetCollection = (Collection) targetValue;
            return targetCollection.contains(sourceValue);
        }
        return targetValue.equals(sourceValue);
    }

    /**
     * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
     * @param item
     * @param mongoRelay
     * @return
     */
    private <A extends Collection<? super TResult>, TResult> Collection<Object> find(A item, MongoRelay mongoRelay) {
        Collection<Object> items = new ArrayList<>();
        if (this.isMultipleFieldReference()) {
            items = this.findReferenceValues(item, this.getReferencesList(), mongoRelay);

        } else {
            FieldReference fieldReference = this.getReferencesList().get(0);
            items = this.findReferenceValues(item, fieldReference, mongoRelay);
        }
        return items;
    }

    private <A extends Collection<? super TResult>, TResult> Collection<Object> findReferenceValues (A item, FieldReference fieldReference, MongoRelay relay) {
        // its a simple equals statement for each of the items
        List<Object> values = item.stream()
                .filter(next -> clazz.isAssignableFrom(next.getClass()))
                .reduce(new ArrayList<>(), (accumulator, next) -> {
                    Object value = relay.getReferenceFields()
                        .getOrDefault(fieldReference.getFormattedSource(), fieldReference.getValue(next));
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
                })
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collection<Object> items = new ArrayList<>();
        if (values.size() == 0) {
            return items;
        }

        Class<?> referencedClass = this.getTargetClass();

        RelayDatabase<?> database = relay.on(referencedClass);
        if (database.getMongoDatabase() == null) {
            return items;
        }
        FindIterable<?> findIterable = database.getCollection().find();
        String field = fieldReference.getMongoTarget(getTargetClass());
        if (fieldReference.hasDynamicTarget()) {
            Object value = relay.getReferenceFields().get(field);
            findIterable.filter(this.filterFrom(field, value)).into(items);
        } else {
            Bson filter = values.size() == 1 ? Filters.eq(field, values.get(0)) : Filters.in(field, values);
            findIterable.filter(filter).into(items);
        }
        return items;
    }

    private <A extends Collection<? super TResult>, TResult> Collection<Object> findReferenceValues (A item, List<FieldReference> fieldReferences, MongoRelay relay) {
        // its a simple equals statement for each of the items
        List<Bson> filters = item.stream()
                .filter(next -> clazz.isAssignableFrom(next.getClass()))
                .map(next -> {
                    List<Bson> matches = fieldReferences.stream()
                            .map(fieldReference -> {
                                Object value = fieldReference.hasDynamicSource() ?
                                    relay.getReferenceFields().get(fieldReference.getFormattedSource()) :
                                    fieldReference.getValue(next);
                                String field = fieldReference.getMongoTarget(getTargetClass());
                                return this.filterFrom(field, value);
                            })
                            .collect(Collectors.toList());
                    if (matches.size() == 0) {
                        return null;
                    }
                    return Filters.and(matches);
                }).filter(Objects::nonNull).collect(Collectors.toList());

        Collection<Object> items = new ArrayList<>();
        if (filters.size() == 0) {
            return items;
        }
        Class<?> referencedClass = this.getTargetClass();
        RelayDatabase<?> db = relay.on(referencedClass);

        // in case the collection is specified in the Reference annotation
        if (!TextUtils.isNullOrEmpty(targetCollection)) {
            db = relay.on(targetCollection, referencedClass);
        }

        RelayFindIterable<?, ?> relayFindIterable = db
                .getCollection()
                .find()
                .filter(Filters.or(filters));

        if (!TextUtils.isNullOrEmpty(projections)) {
            try {
                relayFindIterable.projection(Document.parse(projections));
            } catch (Exception ignore) {}
        }
        relayFindIterable.into(items);
        return items;
    }

    private Bson filterFrom (String field, Object value) {
        if (Objects.isNull(value)) {
            return Filters.exists(field, false);
        } else if (value instanceof Collection) {
            return Filters.in(field, ((Collection<?>) value).toArray());
        }
        return Filters.eq(field, value);
    }
}
