package io.purchaise.mongolay.filters;

import com.mongodb.client.model.Filters;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.conversions.Bson;
import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public @Data class FiltersGroup {
    private String id = UUID.randomUUID().toString();
    private boolean exclude;
    private OperationType operator = OperationType.AND;
    private List<GeneralFilter<?>> filters = new ArrayList<>();

    public Set<Class<?>> getTargets(Map<String, Class<?>> classMap) {
        return filters.stream().map(filter -> Utils.getClassFrom(classMap, filter)).collect(Collectors.toSet());
    }

    /**
     * Should not be called if it's not executable
     * @param relations
     * @param classMap
     * @return
     */
    public Bson execute(Set<CollectionRelation> relations, Map<String, Class<?>> classMap) {
        Assert.assertTrue(this.isExecutable());
        OperationType operator = null;
        Bson acc = null;
        for (GeneralFilter<?> filter: this.getFilters()) {
            if (operator == null) {
                acc = filter.build(relations, classMap);
            } else if (operator == OperationType.AND) {
                acc = Filters.and(acc, filter.build(relations, classMap));
            } else {
                acc = Filters.or(acc, filter.build(relations, classMap));
            }
            operator = filter.getOperator();
        }
        if (this.isExclude()) {
            return Filters.nor(acc);
        }
        return acc;
    }

    @BsonIgnore
    public boolean isExecutable () {
        return !filters.isEmpty();
    }

    @BsonIgnore
    public boolean isOr() {
        if (exclude) {
            return operator == OperationType.AND;
        }
        return operator == OperationType.OR;
    }
}