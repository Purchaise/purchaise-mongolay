package io.purchaise.mongolay;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import lombok.Getter;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by agonlohaj on 07 Aug, 2019
 */
public class RelayDatabase<T> implements MongoDatabase {
	@Getter
	private MongoRelay mongoRelay;
	@Getter
	private MongoDatabase mongoDatabase;

	@Getter
	private Class<T> clazz;
	@Getter
	private String collectionName;

	/**
	 * Returns a new Relay Database
	 * @param mongoRelay
	 * @param mongoDatabase
	 */
	public RelayDatabase(MongoRelay mongoRelay, MongoDatabase mongoDatabase) {
		this.mongoRelay = mongoRelay;
		this.mongoDatabase = mongoDatabase;
	}

	public RelayDatabase<T> withClass(Class<T> clazz) {
		this.clazz = clazz;
		return this;
	}

	public RelayDatabase<T> withCollectionName(String collectionName) {
		this.collectionName = collectionName;
		return this;
	}

	/**
	 * {@link MongoDatabase#getName()}
	 * @return
	 */
	@Override
	public String getName() {
		return mongoDatabase.getName();
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public CodecRegistry getCodecRegistry() {
		return mongoDatabase.getCodecRegistry();
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public ReadPreference getReadPreference() {
		return mongoDatabase.getReadPreference();
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public WriteConcern getWriteConcern() {
		return mongoDatabase.getWriteConcern();
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public ReadConcern getReadConcern() {
		return mongoDatabase.getReadConcern();
	}

	@Override
	public Long getTimeout(TimeUnit timeUnit) {
		return mongoDatabase.getTimeout(timeUnit);
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public MongoDatabase withCodecRegistry(CodecRegistry codecRegistry) {
		return mongoDatabase.withCodecRegistry(codecRegistry);
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public MongoDatabase withReadPreference(ReadPreference readPreference) {
		return mongoDatabase.withReadPreference(readPreference);
	}

	/**
	 * {@link MongoDatabase#getCodecRegistry()}
	 * @return
	 */
	@Override
	public MongoDatabase withWriteConcern(WriteConcern writeConcern) {
		return mongoDatabase.withWriteConcern(writeConcern);
	}

	@Override
	public MongoDatabase withReadConcern(ReadConcern readConcern) {
		return mongoDatabase.withReadConcern(readConcern);
	}

	@Override
	public MongoDatabase withTimeout(long l, TimeUnit timeUnit) {
		return mongoDatabase.withTimeout(l, timeUnit);
	}

	public RelayCollection<T> getCollection() {
		// This returns a collection based on initial class definition and collection name
		return new RelayCollection<>(this, mongoDatabase.getCollection(collectionName, clazz));
	}

	@Override
	@SuppressWarnings("unchecked")
	public RelayCollection<Document> getCollection(String collectionName) {
		return new RelayCollection<>(this, mongoDatabase.getCollection(collectionName));
	}


	@Override
	@SuppressWarnings("unchecked")
	public <TDocument> RelayCollection<TDocument> getCollection(String collectionName, Class<TDocument> tDocumentClass) {
		return new RelayCollection(this, mongoDatabase.getCollection(collectionName, tDocumentClass));
	}

	@Override
	public Document runCommand(Bson command) {
		return mongoDatabase.runCommand(command);
	}

	@Override
	public Document runCommand(Bson command, ReadPreference readPreference) {
		return mongoDatabase.runCommand(command, readPreference);
	}

	@Override
	public <TResult> TResult runCommand(Bson command, Class<TResult> tResultClass) {
		return mongoDatabase.runCommand(command, tResultClass);
	}

	@Override
	public <TResult> TResult runCommand(Bson command, ReadPreference readPreference, Class<TResult> tResultClass) {
		return mongoDatabase.runCommand(command, readPreference, tResultClass);
	}

	@Override
	public Document runCommand(ClientSession clientSession, Bson bson) {
		return mongoDatabase.runCommand(clientSession, bson);
	}

	@Override
	public Document runCommand(ClientSession clientSession, Bson bson, ReadPreference readPreference) {
		return mongoDatabase.runCommand(clientSession, bson, readPreference);
	}

	@Override
	public <TResult> TResult runCommand(ClientSession clientSession, Bson bson, Class<TResult> aClass) {
		return mongoDatabase.runCommand(clientSession, bson, aClass);
	}

	@Override
	public <TResult> TResult runCommand(ClientSession clientSession, Bson bson, ReadPreference readPreference, Class<TResult> aClass) {
		return mongoDatabase.runCommand(clientSession, bson, readPreference, aClass);
	}

	@Override
	public void drop() {
		mongoDatabase.drop();
	}

	@Override
	public void drop(ClientSession clientSession) {
		mongoDatabase.drop(clientSession);
	}

	@Override
	public ListCollectionNamesIterable listCollectionNames() {
		return mongoDatabase.listCollectionNames();
	}

	@Override
	public ListCollectionsIterable<Document> listCollections() {
		return mongoDatabase.listCollections();
	}

	@Override
	public <TResult> ListCollectionsIterable<TResult> listCollections(Class<TResult> tResultClass) {
		return mongoDatabase.listCollections(tResultClass);
	}

	@Override
	public ListCollectionNamesIterable listCollectionNames(ClientSession clientSession) {
		return mongoDatabase.listCollectionNames(clientSession);
	}

	@Override
	public ListCollectionsIterable<Document> listCollections(ClientSession clientSession) {
		return mongoDatabase.listCollections(clientSession);
	}

	@Override
	public <TResult> ListCollectionsIterable<TResult> listCollections(ClientSession clientSession, Class<TResult> aClass) {
		return mongoDatabase.listCollections(clientSession, aClass);
	}

	@Override
	public void createCollection(String collectionName) {
		mongoDatabase.createCollection(collectionName);
	}

	@Override
	public void createCollection(String collectionName, CreateCollectionOptions createCollectionOptions) {
		mongoDatabase.createCollection(collectionName, createCollectionOptions);
	}

	@Override
	public void createCollection(ClientSession clientSession, String collectionName) {
		mongoDatabase.createCollection(clientSession, collectionName);
	}

	@Override
	public void createCollection(ClientSession clientSession, String collectionName, CreateCollectionOptions createCollectionOptions) {
		mongoDatabase.createCollection(clientSession, collectionName, createCollectionOptions);
	}

	@Override
	public void createView(String viewName, String viewOn, List<? extends Bson> pipeline) {
		mongoDatabase.createView(viewName, viewOn, pipeline);
	}

	@Override
	public void createView(String viewName, String viewOn, List<? extends Bson> pipeline, CreateViewOptions createViewOptions) {
		mongoDatabase.createView(viewName, viewOn, pipeline, createViewOptions);
	}

	@Override
	public void createView(ClientSession clientSession, String s, String s1, List<? extends Bson> list) {
		mongoDatabase.createView(clientSession, s, s1, list);

	}

	@Override
	public void createView(ClientSession clientSession, String s, String s1, List<? extends Bson> list, CreateViewOptions createViewOptions) {
		mongoDatabase.createView(clientSession, s, s1, list, createViewOptions);
	}

	@Override
	public ChangeStreamIterable<Document> watch() {
		return mongoDatabase.watch();
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> aClass) {
		return mongoDatabase.watch(aClass);
	}

	@Override
	public ChangeStreamIterable<Document> watch(List<? extends Bson> list) {
		return mongoDatabase.watch(list);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> list, Class<TResult> aClass) {
		return mongoDatabase.watch(list, aClass);
	}

	@Override
	public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
		return mongoDatabase.watch(clientSession);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> aClass) {
		return mongoDatabase.watch(clientSession, aClass);
	}

	@Override
	public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> list) {
		return mongoDatabase.watch(clientSession, list);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
		return mongoDatabase.watch(clientSession, list, aClass);
	}

	@Override
	public AggregateIterable<Document> aggregate(List<? extends Bson> list) {
		return mongoDatabase.aggregate(list);
	}

	@Override
	public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> list, Class<TResult> aClass) {
		return mongoDatabase.aggregate(list, aClass);
	}

	@Override
	public AggregateIterable<Document> aggregate(ClientSession clientSession, List<? extends Bson> list) {
		return mongoDatabase.aggregate(clientSession, list);
	}

	@Override
	public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
		return mongoDatabase.aggregate(clientSession, list, aClass);
	}
}
