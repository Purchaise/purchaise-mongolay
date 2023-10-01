package io.purchaise.mongolay;

import com.mongodb.client.MongoDatabase;
import io.purchaise.mongolay.utils.ClassUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Created by agonlohaj on 07 Aug, 2019
 */
@AllArgsConstructor
public class MongoRelayClient extends MongoRelay {
	/**
	 * The global mongo collection names
	 */
	public static final List<String> GLOBAL_COLLECTIONS_NAMES =
			unmodifiableList(asList(
				"password_resets",
				"feedback",
				"logger",
				"invites",
				"organisations",
				"tracker",
				"users",
				"tokens",
				"login_logger",
				"example_person"
			));

	/**
	 * Global mongo database, for global accessed classes
	 */
	@Getter
	MongoDatabase global;

	/**
	 * Constructs a new Mongo Relay, given only the factory, useful on Global Collections without ACL control
	 */
	public MongoRelayClient(MongoDatabase local, MongoDatabase global) {
		super(local);
		this.global = global;
	}


	/**
	 * Constructs a new Mongo Relay, given the user, has access to user database, and can enforce ACL
	 * @param copy
	 */
	public MongoRelayClient(MongoRelayClient copy) {
		super(copy);
		this.global = copy.global;
	}

	@Override
	public MongoRelay copy () {
		return new MongoRelayClient(this);
	}

	@Override
	public MongoRelay withDepth(int depth) {
		super.withDepth(depth);
		return this;
	}

	@Override
	public MongoRelay withMaxDepth(int depth) {
		super.withMaxDepth(depth);
		return this;
	}

	private MongoDatabase globalMongo (String collectionName) {
		if (GLOBAL_COLLECTIONS_NAMES.contains(collectionName)) {
			return global;
		}
		return null;
	}


	private <T> MongoDatabase globalMongo (Class<T> clazz) {
		String entityName = ClassUtils.entityName(clazz);
		if (entityName == null || entityName.equals("")) {
			return null;
		}
		return globalMongo(entityName);
	}

	@Override
	protected  <T> MongoDatabase mongoDatabaseOn(Class<T> clazz) {
		MongoDatabase database = this.globalMongo(clazz);
		if (database == null) {
			// otherwise it has to be normal one
			return this.getDatabase();
		}
		return database;
	}

	@Override
	protected MongoDatabase mongoDatabaseOn(String collectionName) {
		MongoDatabase database = this.globalMongo(collectionName);
		if (database == null) {
			// otherwise it has to be normal one
			return this.getDatabase();
		}
		return database;
	}
}
