package io.purchaise.mongolay;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Collation;
import lombok.Getter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

/**
 * Created by agonlohaj on 08 Aug, 2019
 */
public class RelayAggregation<TDocument, TResult> extends RelayMongoIterable<TDocument, TResult> implements AggregateIterable<TResult> {
	@Getter
	private RelayCollection<TDocument> relayCollection;
	@Getter
	private AggregateIterable<TResult> aggregateIterable;

	public RelayAggregation(RelayCollection<TDocument> collection, AggregateIterable<TResult> aggregateIterable, Class<TResult> clazz) {
		super(collection, aggregateIterable, clazz);
		this.relayCollection = collection;
		this.aggregateIterable = aggregateIterable;
	}

	@Override
	public void toCollection() {
		aggregateIterable.toCollection();
	}

	@Override
	public AggregateIterable<TResult> allowDiskUse(Boolean allowDiskUse) {
		this.aggregateIterable = aggregateIterable.allowDiskUse(allowDiskUse);
		return this;
	}

	@Override
	public AggregateIterable<TResult> batchSize(int batchSize) {
		super.batchSize(batchSize);
		return this;
	}

	@Override
	public AggregateIterable<TResult> maxTime(long maxTime, TimeUnit timeUnit) {
		this.aggregateIterable = aggregateIterable.maxTime(maxTime, timeUnit);
		return this;
	}

	@Override
	public AggregateIterable<TResult> maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
		this.aggregateIterable = aggregateIterable.maxAwaitTime(maxAwaitTime, timeUnit);
		return this;
	}

	@Override
	public AggregateIterable<TResult> bypassDocumentValidation(Boolean bypassDocumentValidation) {
		this.aggregateIterable = aggregateIterable.bypassDocumentValidation(bypassDocumentValidation);
		return this;
	}

	@Override
	public AggregateIterable<TResult> collation(Collation collation) {
		this.aggregateIterable = aggregateIterable.collation(collation);
		return this;
	}

	@Override
	public AggregateIterable<TResult> comment(String comment) {
		this.aggregateIterable = aggregateIterable.comment(comment);
		return this;
	}

	@Override
	public AggregateIterable<TResult> comment(BsonValue bsonValue) {
		this.aggregateIterable = aggregateIterable.comment(bsonValue);
		return this;
	}

	@Override
	public AggregateIterable<TResult> hint(Bson hint) {
		this.aggregateIterable = aggregateIterable.hint(hint);
		return this;
	}

	@Override
	public AggregateIterable<TResult> hintString(String s) {
		this.aggregateIterable = aggregateIterable.hintString(s);
		return this;
	}

	@Override
	public AggregateIterable<TResult> let(Bson bson) {
		this.aggregateIterable = aggregateIterable.let(bson);
		return this;
	}

	@Override
	public Document explain() {
		return aggregateIterable.explain();
	}

	@Override
	public Document explain(ExplainVerbosity explainVerbosity) {
		return aggregateIterable.explain(explainVerbosity);
	}

	@Override
	public <E> E explain(Class<E> aClass) {
		return aggregateIterable.explain(aClass);
	}

	@Override
	public <E> E explain(Class<E> aClass, ExplainVerbosity explainVerbosity) {
		return aggregateIterable.explain(aClass, explainVerbosity);
	}
}
