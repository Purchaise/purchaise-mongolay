package io.purchaise.mongolay;

import com.mongodb.Function;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import lombok.Getter;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by agonlohaj on 08 Aug, 2019
 */
public class RelayMongoIterable<TDocument, TResult> implements MongoIterable<TResult> {
	@Getter
	private RelayCollection<TDocument> relayCollection;
	@Getter
	private MongoIterable<TResult> mongoIterable;
	@Getter
	private Class<TResult> clazz;

	public MongoRelay getMongoRelay () {
		return relayCollection.getMongoRelay();
	}

	public RelayMongoIterable(RelayCollection<TDocument> collection, MongoIterable<TResult> mongoIterable, Class<TResult> clazz) {
		this.relayCollection = collection;
		this.mongoIterable = mongoIterable;
		this.clazz = clazz;
	}

	@Override
	public MongoCursor<TResult> iterator() {
		return new RelayCursor<>(this, mongoIterable.iterator());
	}

	@Override
	public MongoCursor<TResult> cursor() {
		return new RelayCursor<>(this, mongoIterable.cursor());
	}

	@Override
	public TResult first() {
		TResult result = mongoIterable.first();
		if (result == null) {
			return null;
		}
		return getMongoRelay().map(result, relayCollection.getDocumentClass());
	}


	public CompletableFuture<TResult> first(Executor context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.first();
			} catch (Exception e) {
				e.printStackTrace();
				throw new CompletionException(e);
			}
		}, context);
	}

	@Override
	public <U> MongoIterable<U> map(Function<TResult, U> mapper) {
		return new RelayMongoIterable(relayCollection, mongoIterable.map(mapper), clazz);
	}

	@Override
	public void forEach(Consumer<? super TResult> block) {
		mongoIterable.forEach(block);
	}

	@Override
	public <A extends Collection<? super TResult>> A into(A target) {
		mongoIterable.into(target);
		return getMongoRelay().map(target, getClazz());
	}

	public <A extends Collection<? super TResult>> CompletableFuture<A> into(A target, Executor context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.into(target);
			} catch (Exception e) {
				e.printStackTrace();
				throw new CompletionException(new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable"));
			}
		}, context);
	}

	@Override
	public MongoIterable<TResult> batchSize(int batchSize) {
		return mongoIterable.batchSize(batchSize);
	}
}
