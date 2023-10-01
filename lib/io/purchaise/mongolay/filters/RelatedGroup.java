package io.purchaise.mongolay.filters;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import lombok.*;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public @Data class RelatedGroup {
    private OperationType operator = OperationType.AND;
    List<FiltersGroup> groups = new ArrayList<>();

    public RelatedGroup(FiltersGroup group) {
        this.operator = group.getOperator();
        this.groups.add(group);
    }

    public Set<Class<?>> getTargets(Map<String, Class<?>> classMap) {
        return groups.stream().reduce(new HashSet<>(), (acc, next) -> {
            acc.addAll(next.getTargets(classMap));
            return acc;
        }, (a, b) -> (HashSet<Class<?>>) Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet()));
    }

    /**
     * Check if this group requires a dependency with a collection
     * @param collection
     * @return
     */
    public boolean isRequired(Class<?> collection, Map<String, Class<?>> classMap) {
        return getGroups().stream()
                .filter(FiltersGroup::isExecutable) // there has to be at least a filter
                .anyMatch((group) -> group.getFilters().stream().anyMatch(filter -> Objects.equals(Utils.getClassFrom(classMap, filter), collection)));
    }


    /**
     * Check if this group is executable given the linked collections
     * @param linkedCollections
     * @param classMap
     * @return
     */
    public boolean isExecutable(Set<Class<?>> linkedCollections, Map<String, Class<?>> classMap) {
        boolean executable = getGroups().stream().anyMatch(FiltersGroup::isExecutable);
        return linkedCollections.containsAll(getTargets(classMap)) && executable;
    }

    public Bson execute(Set<CollectionRelation> relations, Map<String, Class<?>> classMap) {
        List<Bson> filters = getGroups().stream()
                .filter(FiltersGroup::isExecutable)
                .map(next -> next.execute(relations, classMap))
                .collect(Collectors.toList());
        if (filters.size() == 1) {
            // AND or OR it is the same
            return Aggregates.match(filters.get(0));
        }
        if (operator == OperationType.OR) {
            return Aggregates.match(Filters.or(filters));
        }
        return Aggregates.match(Filters.and(filters));
    }
}