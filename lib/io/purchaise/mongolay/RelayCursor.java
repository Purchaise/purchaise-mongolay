package io.purchaise.mongolay;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by agonlohaj on 08 Aug, 2019
 */
@AllArgsConstructor
public class RelayCursor<TDocument, TResult> implements MongoCursor<TResult> {
	@Getter
	private RelayMongoIterable<TDocument, TResult> relayMongoIterable;
	@Getter
	private MongoCursor<TResult> cursor;

	@Override
	public void close() {
		cursor.close();
	}

	@Override
	public boolean hasNext() {
		return cursor.hasNext();
	}

	@Override
	public TResult next() {
		return relayMongoIterable.getMongoRelay().map(cursor.next(), relayMongoIterable.getClazz());
	}

	@Override
	public TResult tryNext() {
		TResult result = cursor.tryNext();
		if (result == null) {
			return null;
		}
		return relayMongoIterable.getMongoRelay().map(result, relayMongoIterable.getClazz());
	}

	@Override
	public ServerCursor getServerCursor() {
		return cursor.getServerCursor();
	}

	@Override
	public ServerAddress getServerAddress() {
		return cursor.getServerAddress();
	}
}
