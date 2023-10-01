package io.purchaise.mongolay.filters;


import com.mongodb.client.model.Filters;
import io.purchaise.mongolay.utils.TextUtils;
import lombok.*;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.conversions.Bson;
import org.junit.Assert;

import java.time.*;
import java.util.*;

@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public @Data class GeneralFilter<T> implements Cloneable {
	private String id = UUID.randomUUID().toString();
	private CriteriaType type = CriteriaType.STRING;
	private ConditionType condition = ConditionType.EQ;
	private OperationType operator = OperationType.AND;
	//TODO: always provide from front-end side
	private String collection;
	private boolean exclude = false;
	private boolean inArrayFilter = false;
	// make a getter that returns the class from enum
	private String column;
	// for other cases its this criteria
	private T criteria;
	/**
	 * Field Name to apply elemMatch on
	 */
	private String elemMatchField;


	public String getColumn(Set<CollectionRelation> relations, Map<String, Class<?>> classMap) {
		Class<?> clazz = Utils.getClassFrom(classMap, this);
		Optional<CollectionRelation> relation = relations.stream().filter(next -> next.getTo().equals(clazz)).findAny();
		return relation.isEmpty() ? this.getColumn() : String.format("%s.%s", relation.get().getAs(), getColumn());
	}
	@BsonIgnore
	public boolean isOr() {
		return operator == OperationType.OR;
	}


	/**
	 * Constructs Bson filter corresponding a general filter model
	 * @param relations relations between collections
	 * @param classMap the class map
	 */
	@BsonIgnore
	public Bson build(Set<CollectionRelation> relations, Map<String, Class<?>> classMap) {
		Bson bsonFilter = this.buildFilter(relations, classMap);
		if (this.isExclude()) {
			return Filters.nor(bsonFilter);
		}
		return bsonFilter;
	}

	/**
	 * Constructs Bson filter corresponding a general filter model
	 * @param relations relations between collections
	 * @param classMap the class map
	 * @return Bson filter
	 */
	public <T> Bson buildFilter(Set<CollectionRelation> relations, Map<String, Class<?>> classMap) {
		String column = this.getColumn(relations, classMap);
		Bson filter = this.buildFilter(column);
		return !TextUtils.isNullOrEmpty(elemMatchField) ? Filters.elemMatch(elemMatchField, filter) : filter;
	}

	private Bson buildFilter(String column) {
		switch (this.getCondition()) {
			case LT:
				return Filters.lt(column, this.getCriteria());
			case LTE:
				return Filters.lte(column, this.getCriteria());
			case GT:
				return Filters.gt(column, this.getCriteria());
			case GTE:
				return Filters.gte(column, this.getCriteria());
			case BT: {
				Assert.assertTrue(this.getCriteria() instanceof List<?>);
				List<?> list = (List<?>) this.getCriteria();
				Assert.assertEquals(2, list.size());
				return new Document(column, new Document("$gte", list.get(0)).append("$lte", list.get(1)));
			}
			case NBT: {
				Assert.assertTrue(this.getCriteria() instanceof List<?>);
				List<?> list = (List<?>) this.getCriteria();
				Assert.assertEquals(2, list.size());
				return Filters.or(
						Filters.lt(column, list.get(0)),
						Filters.gt(column, list.get(1))
				);
			}
			case CONTAINS:
				return Filters.regex(column, "(?i)" + this.getCriteria().toString().replace("*", "\\*").trim());
			case NCONTAINS:
				return Filters.not(Filters.regex(column, "(?i)" + this.getCriteria().toString().replace("*", "\\*").trim()));
			case IN: {
				Assert.assertTrue(this.getCriteria() instanceof List<?>);
				List<?> list = (List<?>) this.getCriteria();
				return Filters.in(column, list);
			}
			case NIN: {
				Assert.assertTrue(this.getCriteria() instanceof List<?>);
				List<?> list = (List<?>) this.getCriteria();
				return Filters.nin(column, list);
			}
			case EXIST:
				return Filters.exists(column);
			case NE:
				return Filters.ne(column, this.getCriteria());
			case EQ:
			default:
				if (this.getType().equals(CriteriaType.DATE)) {
					return this.compareTimestamps();
				}
				return Filters.eq(column, this.getCriteria());
		}
	}

	/**
	 * Determines if two timestamps belong to a same day.
	 */
	private Bson compareTimestamps() {
		ZoneOffset zero = ZoneOffset.ofHours(0);
		LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli((Long) this.getCriteria()), zero);
		Long startOfDay = LocalDateTime.of(date, LocalTime.MIN).toInstant(zero).toEpochMilli();
		Long endOfDay = LocalDateTime.of(date, LocalTime.MAX).toInstant(zero).toEpochMilli();
		return new Document(this.getColumn(), new Document("$gte", startOfDay).append("$lte", endOfDay));
	}

	@Override
	public GeneralFilter<T> clone() {
		try {
			GeneralFilter<T> clone = (GeneralFilter<T>) super.clone();
			clone.setId(this.getId());
			clone.setType(this.getType());
			clone.setCondition(this.getCondition());
			clone.setOperator(this.getOperator());
			clone.setCollection(this.getCollection());
			clone.setColumn(this.getColumn());
			clone.setCriteria(this.getCriteria());
			clone.setElemMatchField(this.getElemMatchField());
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
