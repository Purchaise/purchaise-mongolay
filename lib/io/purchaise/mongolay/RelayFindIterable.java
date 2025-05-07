package io.purchaise.mongolay;

import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.cursor.TimeoutMode;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by agonlohaj on 08 Aug, 2019
 */
public class RelayFindIterable<TDocument, TResult> extends RelayMongoIterable<TDocument, TResult> implements FindIterable<TResult> {
	@Getter
	private RelayCollection<TDocument> relayCollection;
	@Getter
	private FindIterable<TResult> findIterable;

	private boolean access = true;


	public MongoRelay getMongoRelay () {
		return relayCollection.getMongoRelay();
	}

	public MongoDatabase getMongoDatabase () {
		return relayCollection.getMongoDatabase();
	}

	public RelayFindIterable(RelayCollection<TDocument> collection, FindIterable<TResult> findIterable, Class<TResult> clazz) {
		super(collection, findIterable, clazz);
		this.relayCollection = collection;
		this.findIterable = findIterable;
		this.filter(null);
	}

	@Override
	public RelayFindIterable<TDocument, TResult> filter(Bson filter) {
		List<Bson> filters = this.extractFilters(filter);
		if (filters.isEmpty()) {
			return this;
		}
		if (filters.size() == 1) {
			findIterable = findIterable.filter(filters.get(0));
			return this;
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		findIterable = findIterable.filter(Filters.and(filters));
		return this;
	}

	/**
	 * Removes the access flag, and resets filtering
	 * @return RelayFindIterable<TDocument, TResult>
	 */
	public RelayFindIterable<TDocument, TResult> withoutAccess() {
		this.access = false;
		this.filter(null);
		return this;
	}


	@Override
	public RelayFindIterable<TDocument, TResult> limit(int limit) {
		this.findIterable = findIterable.limit(limit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> skip(int skip) {
		this.findIterable = findIterable.skip(skip);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> maxTime(long maxTime, TimeUnit timeUnit) {
		this.findIterable = findIterable.maxTime(maxTime, timeUnit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
		this.findIterable = findIterable.maxAwaitTime(maxAwaitTime, timeUnit);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> projection(Bson projection) {
		this.findIterable = findIterable.projection(projection);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> sort(Bson sort) {
		this.findIterable = findIterable.sort(sort);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> noCursorTimeout(boolean noCursorTimeout) {
		this.findIterable = findIterable.noCursorTimeout(noCursorTimeout);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> partial(boolean partial) {
		this.findIterable = findIterable.partial(partial);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> cursorType(CursorType cursorType) {
		this.findIterable = findIterable.cursorType(cursorType);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> collation(Collation collation) {
		this.findIterable = findIterable.collation(collation);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> comment(String comment) {
		this.findIterable = findIterable.comment(comment);
		return this;
	}

	@Override
	public FindIterable<TResult> comment(BsonValue bsonValue) {
		this.findIterable = findIterable.comment(bsonValue);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> hint(Bson hint) {
		this.findIterable = findIterable.hint(hint);
		return this;
	}

	@Override
	public FindIterable<TResult> hintString(String hint) {
		this.findIterable = findIterable.hintString(hint);
		return this;
	}

	@Override
	public FindIterable<TResult> let(Bson bson) {
		this.findIterable = findIterable.let(bson);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> max(Bson max) {
		this.findIterable = findIterable.max(max);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> min(Bson min) {
		this.findIterable = findIterable.min(min);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> returnKey(boolean returnKey) {
		this.findIterable = findIterable.returnKey(returnKey);
		return this;
	}

	@Override
	public RelayFindIterable<TDocument, TResult> showRecordId(boolean showRecordId) {
		this.findIterable = findIterable.showRecordId(showRecordId);
		return this;
	}

	@Override
	public FindIterable<TResult> allowDiskUse(Boolean allowDiskUse) {
		this.findIterable = findIterable.allowDiskUse(allowDiskUse);
		return this;
	}

	@Override
	public FindIterable<TResult> timeoutMode(TimeoutMode timeoutMode) {
		this.findIterable = findIterable.timeoutMode(timeoutMode);
		return this;
	}

	@Override
	public Document explain() {
		return findIterable.explain();
	}

	@Override
	public Document explain(ExplainVerbosity explainVerbosity) {
		return findIterable.explain(explainVerbosity);
	}

	@Override
	public <E> E explain(Class<E> aClass) {
		return findIterable.explain(aClass);
	}

	@Override
	public <E> E explain(Class<E> aClass, ExplainVerbosity explainVerbosity) {
		return findIterable.explain(aClass, explainVerbosity);
	}

	@Override
	public MongoCursor<TResult> cursor() {
		return new RelayCursor<>(this, findIterable.cursor());
	}

	@Override
	public RelayFindIterable<TDocument, TResult> batchSize(int batchSize) {
		this.findIterable = findIterable.batchSize(batchSize);
		return this;
	}

	protected List<Bson> extractFilters (Bson filter) {
		List<Bson> filters = new ArrayList<>();
		if (access) {
			filters = getMongoRelay().acl(relayCollection.getDocumentClass(), getRelayCollection().getDatabase().getCollectionName());
		}
		if (filter != null) {
			filters.add(filter);
		}
		return filters;
	}
}
