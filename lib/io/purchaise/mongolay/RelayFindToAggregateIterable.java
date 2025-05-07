package io.purchaise.mongolay;

import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.internal.client.model.FindOptions;
import io.purchaise.mongolay.annotations.Index;
import lombok.Getter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by agonlohaj on 31 Oct, 2024
 */
@Getter
public class RelayFindToAggregateIterable<TDocument, TResult> extends RelayFindIterable<TDocument, TResult> {
	private final FindOptions findOptions;
	private Bson filter;

	public RelayFindToAggregateIterable(RelayCollection<TDocument> collection, FindIterable<TResult> findIterable, Class<TResult> clazz) {
		super(collection, findIterable, clazz);
		this.findOptions = new FindOptions();
	}

	@Override
	protected List<Bson> extractFilters (Bson filter) {
		List<Bson> filters = super.extractFilters(filter);
		if (filters.size() == 1) {
			this.filter = filters.get(0);
		} else if (filters.size() > 1) {
			this.filter = Filters.and(filters);
		}
		return filters;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> limit(int limit) {
		this.findOptions.limit(limit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> skip(int skip) {
		this.findOptions.skip(skip);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> maxTime(long maxTime, TimeUnit timeUnit) {
		this.findOptions.maxTime(maxTime, timeUnit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
		this.findOptions.maxAwaitTime(maxAwaitTime, timeUnit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> projection(Bson projection) {
		this.findOptions.projection(projection);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> sort(Bson sort) {
		this.findOptions.sort(sort);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> noCursorTimeout(boolean noCursorTimeout) {
		this.findOptions.noCursorTimeout(noCursorTimeout);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> partial(boolean partial) {
		this.findOptions.partial(partial);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> cursorType(CursorType cursorType) {
		this.findOptions.cursorType(cursorType);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> collation(Collation collation) {
		this.findOptions.collation(collation);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> comment(String comment) {
		this.findOptions.comment(comment);
		return this;
	}

	@Override
	public FindIterable<TResult> comment(BsonValue bsonValue) {
		this.findOptions.comment(bsonValue);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> hint(Bson hint) {
		this.findOptions.hint(hint);
		return this;
	}

	@Override
	public FindIterable<TResult> hintString(String hint) {
		this.findOptions.hintString(hint);
		return this;
	}

	@Override
	public FindIterable<TResult> let(Bson bson) {
		this.findOptions.let(bson);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> max(Bson max) {
		this.findOptions.max(max);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> min(Bson min) {
		this.findOptions.min(min);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> returnKey(boolean returnKey) {
		this.findOptions.returnKey(returnKey);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> showRecordId(boolean showRecordId) {
		this.findOptions.showRecordId(showRecordId);
		return this;
	}

	@Override
	public FindIterable<TResult> allowDiskUse(Boolean allowDiskUse) {
		this.findOptions.allowDiskUse(allowDiskUse);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> batchSize(int batchSize) {
		this.findOptions.batchSize(batchSize);
		return this;
	}

	@Override
	public Document explain() {
		return getExplainResult(Document.class, null);
	}

	@Override
	public Document explain(ExplainVerbosity explainVerbosity) {
		return getExplainResult(Document.class, explainVerbosity);
	}

	@Override
	public <E> E explain(Class<E> resultClass) {
		return getExplainResult(resultClass, null);
	}

	@Override
	public <E> E explain(Class<E> resultClass, ExplainVerbosity explainVerbosity) {
		return getExplainResult(resultClass, explainVerbosity);
	}

	private <E> E getExplainResult(Class<E> resultClass, ExplainVerbosity explainVerbosity) {
		RelayAggregation<TDocument, TResult> iterable = this.adaptToAggregateIterable();
		return explainVerbosity == null ? iterable.explain(resultClass) : iterable.explain(resultClass, explainVerbosity);
	}

	@Override
	public MongoCursor<TResult> iterator() {
		return this.adaptToAggregateIterable().iterator();
	}

	@Override
	public MongoCursor<TResult> cursor() {
		return this.adaptToAggregateIterable().cursor();
	}

	@Override
	public void forEach(Consumer<? super TResult> block) {
		this.adaptToAggregateIterable().forEach(block);
	}

	@Override
	public <U> MongoIterable<U> map(Function<TResult, U> mapper) {
		return this.adaptToAggregateIterable().map(mapper);
	}

	@Override
	public TResult first() {
		return this.adaptToAggregateIterable().first();
	}

	@Override
	public <A extends Collection<? super TResult>> A into(A target) {
		return this.adaptToAggregateIterable().into(target);
	}

    private RelayAggregation<TDocument, TResult> adaptToAggregateIterable() {
        List<Bson> pipeline = new ArrayList<>();
        FindOptions findOptions = this.getFindOptions();

        // The part that is handled with an aggregation pipeline
        Optional.ofNullable(this.getFilter()).ifPresent(filter -> pipeline.add(Aggregates.match(filter)));
        Optional.ofNullable(findOptions.getMin()).ifPresent(min -> pipeline.add(Aggregates.match(min)));
        Optional.ofNullable(findOptions.getMax()).ifPresent(max -> pipeline.add(Aggregates.match(max)));
        Optional.ofNullable(findOptions.getSort()).ifPresent(sort -> pipeline.add(Aggregates.sort(sort)));
        Optional.ofNullable(findOptions.getProjection()).ifPresent(projection -> {
            if (findOptions.isShowRecordId()) {
                pipeline.add(Aggregates.project(Projections.fields(projection, Projections.include("_id"))));
                return;
            }
            pipeline.add(Aggregates.project(projection));
        });
        Optional.of(findOptions.isReturnKey())
            .filter(returnKey -> returnKey)
            .ifPresent(returnKey -> {
                List<String> indexedFields = FieldUtils.getFieldsListWithAnnotation(this.getClazz(), Index.class)
                    .stream()
                    .map(Field::getName)
                    .collect(Collectors.toList());
                pipeline.add(Aggregates.project(Projections.include(indexedFields)));
            });
        Optional.of(findOptions.getSkip()).ifPresent(skip -> pipeline.add(Aggregates.skip(skip)));
        Optional.of(findOptions.getLimit())
            .filter(limit -> limit > 0)
            .ifPresent(limit -> pipeline.add(Aggregates.limit(limit)));

        RelayAggregation<TDocument, TResult> relayAggregation = new RelayAggregation<>(
            this.getRelayCollection(),
            this.getRelayCollection().aggregate(pipeline, this.getClazz()),
            this.getClazz()
        );

        // Set remaining options on relayAggregation
        Optional.of(findOptions.getBatchSize()).ifPresent(relayAggregation::batchSize);
        Optional.of(findOptions.getMaxTime(TimeUnit.MILLISECONDS)).ifPresent(maxTimeMs -> relayAggregation.maxTime(maxTimeMs, TimeUnit.MILLISECONDS));
        Optional.of(findOptions.getMaxAwaitTime(TimeUnit.MILLISECONDS)).ifPresent(maxAwaitTimeMs -> relayAggregation.maxAwaitTime(maxAwaitTimeMs, TimeUnit.MILLISECONDS));
        Optional.ofNullable(findOptions.getCollation()).ifPresent(relayAggregation::collation);
        Optional.ofNullable(findOptions.getComment()).ifPresent(relayAggregation::comment);
        Optional.ofNullable(findOptions.getHint()).ifPresent(relayAggregation::hint);
        Optional.ofNullable(findOptions.getHintString()).ifPresent(relayAggregation::hintString);
        Optional.ofNullable(findOptions.getLet()).ifPresent(relayAggregation::let);
        Optional.ofNullable(findOptions.isAllowDiskUse()).ifPresent(relayAggregation::allowDiskUse);

        // Handle non-supported options
        String warningMessage = "Warning: The '%s' option is not supported in aggregation queries. This option will be ignored.\n";
        Optional.of(findOptions.getCursorType()).ifPresent(cursorType -> System.out.printf(warningMessage, "cursorType"));
        if (findOptions.isNoCursorTimeout()) {
            System.out.printf(warningMessage, "noCursorTimeout");
        }
        if (findOptions.isPartial()) {
            System.out.printf(warningMessage, "partial");
        }

        return relayAggregation;
    }
}
