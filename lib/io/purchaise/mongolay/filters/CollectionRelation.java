package io.purchaise.mongolay.filters;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import io.purchaise.mongolay.utils.ClassUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by agonlohaj on 14 Sep, 2022
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionRelation {
	private Class<?> from;
	private Class<?> to;
	private String lookupField;
	private String foreignField;
	private String as;
	private boolean single;

//	public String getCollectionFrom() {
//		return ClassUtils.entityName(from);
//	}

	public String getCollectionTo() {
		return ClassUtils.entityName(to);
	}

	public List<Bson> build() {
		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.lookup(getCollectionTo(), getLookupField(), getForeignField(), getAs()));
		if (isSingle()) {
			pipeline.add(Aggregates.addFields(new Field<>(getAs(), new Document("$first", String.format("$%s", getAs())))));
		}
		return pipeline;
	}
}
