package io.purchaise.mongolay;

import com.mongodb.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.ClientSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Created by agonlohaj on 08 Aug, 2019
 */
@AllArgsConstructor
public class RelayCollection<TDocument> implements MongoCollection<TDocument> {
	@Getter
	private RelayDatabase database;
	@Getter
	private MongoCollection<TDocument> collection;

	public MongoRelay getMongoRelay () {
		return database.getMongoRelay();
	}

	public MongoDatabase getMongoDatabase () {
		return database.getMongoDatabase();
	}

	/**
	 * Validates a value of a class, by checking Hibernate Validation
	 * And also Access control based on definitions
	 * @param value
	 * @return
	 * @throws RelayException
	 */
	@SuppressWarnings("unchecked")
	public <TDocument> TDocument validate (TDocument value, Bson filter) throws RelayException {
		return this.validate(value, filter, true);
	}

	/**
	 * Validates a value of a class, by checking Hibernate Validation
	 * And also Access control based on definitions
	 * @param value
	 * @return
	 * @throws RelayException
	 */
	@SuppressWarnings("unchecked")
	public <TDocument> TDocument validate (TDocument value, Bson filter, boolean withModel) throws RelayException {
		return getMongoRelay().validate(value, (RelayCollection<TDocument>) this, filter, withModel);
	}


	/**
	 * Validates a value of a class, by checking Hibernate Validation
	 * And also Access control based on definitions
	 * @param value
	 * @return
	 * @throws RelayException
	 */
	public TDocument validate (ObjectId value, Bson filter) throws RelayException {
		return getMongoRelay().validate(value, this, filter);
	}


	/**
	 * Validates a value of a class, by checking Hibernate Validation
	 * And also Access control based on definitions
	 * @param key
	 * @return
	 * @throws RelayException
	 */
	public TDocument validate (Bson key, Bson filter) throws RelayException {
		return getMongoRelay().validate(key, this, filter);
	}

	@Override
	public MongoNamespace getNamespace() {
		return collection.getNamespace();
	}

	@Override
	public Class<TDocument> getDocumentClass() {
		return collection.getDocumentClass();
	}

	@Override
	public CodecRegistry getCodecRegistry() {
		return collection.getCodecRegistry();
	}

	@Override
	public ReadPreference getReadPreference() {
		return collection.getReadPreference();
	}

	@Override
	public WriteConcern getWriteConcern() {
		return collection.getWriteConcern();
	}

	@Override
	public ReadConcern getReadConcern() {
		return collection.getReadConcern();
	}

	@Override
	public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
		return collection.withDocumentClass(clazz);
	}

	@Override
	public MongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
		return collection.withCodecRegistry(codecRegistry);
	}

	@Override
	public MongoCollection<TDocument> withReadPreference(ReadPreference readPreference) {
		return collection.withReadPreference(readPreference);
	}

	@Override
	public MongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern) {
		return collection.withWriteConcern(writeConcern);
	}

	@Override
	public MongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
		return collection.withReadConcern(readConcern);
	}

	public List<Bson> filterWithAcl(Bson filter) {
		List<Bson> filters = getMongoRelay().acl(getDocumentClass(), getDatabase().getCollectionName());
		if (filter != null) {
			filters.add(filter);
		}
		return filters;
	}

	public long count() {
		return this.countDocuments();
	}

	@Override
	public long countDocuments() {
		List<Bson> filters = this.filterWithAcl(null);
		if (filters.size() == 0) {
			return collection.countDocuments();
		}
		if (filters.size() == 1) {
			return collection.countDocuments(filters.get(0));
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(Filters.and(filters));
	}

	public long count(Bson filter) {
		return this.countDocuments(filter);
	}

	@Override
	public long countDocuments(Bson filter) {
		List<Bson> filters = this.filterWithAcl(filter);
		if (filters.size() == 0) {
			return collection.countDocuments();
		}
		if (filters.size() == 1) {
			return collection.countDocuments(filters.get(0));
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(Filters.and(filters));
	}

	public long count(Bson filter, CountOptions options) {
		return this.countDocuments(filter, options);
	}

	@Override
	public long countDocuments(Bson filter, CountOptions options) {
		List<Bson> filters = this.filterWithAcl(filter);
		if (filters.size() == 0) {
			return collection.countDocuments();
		}
		if (filters.size() == 1) {
			return collection.countDocuments(filters.get(0), options);
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(Filters.and(filters), options);
	}

	public long count(ClientSession clientSession) {
		return this.countDocuments(clientSession);
	}

	@Override
	public long countDocuments(ClientSession clientSession) {
		List<Bson> filters = this.filterWithAcl(null);
		if (filters.size() == 0) {
			return collection.countDocuments(clientSession);
		}
		if (filters.size() == 1) {
			return collection.countDocuments(clientSession, filters.get(0));
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(clientSession, Filters.and(filters));
	}

	public long count(ClientSession clientSession, Bson filter) {
		return this.countDocuments(clientSession, filter);
	}

	@Override
	public long countDocuments(ClientSession clientSession, Bson filter) {
		List<Bson> filters = this.filterWithAcl(filter);
		if (filters.size() == 0) {
			return collection.countDocuments(clientSession);
		}
		if (filters.size() == 1) {
			return collection.countDocuments(clientSession, filters.get(0));
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(clientSession, Filters.and(filters));
	}

	public long count(ClientSession clientSession, Bson filter, CountOptions options) {
		return this.countDocuments(clientSession, filter, options);
	}

	@Override
	public long countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
		List<Bson> filters = this.filterWithAcl(filter);
		if (filters.size() == 0) {
			return collection.countDocuments(clientSession);
		}
		if (filters.size() == 1) {
			return collection.countDocuments(clientSession, filters.get(0), options);
		}
		// check if ACL is turned on, and whether the Mongo Relay has something to say abut this filtering!
		return collection.countDocuments(clientSession, Filters.and(filters), options);
	}

	@Override
	public long estimatedDocumentCount() {
		return collection.estimatedDocumentCount();
	}

	@Override
	public long estimatedDocumentCount(EstimatedDocumentCountOptions estimatedDocumentCountOptions) {
		return collection.estimatedDocumentCount(estimatedDocumentCountOptions);
	}

	@Override
	public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> tResultClass) {
		return collection.distinct(fieldName, tResultClass);
	}

	@Override
	public <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> tResultClass) {
		return collection.distinct(fieldName, filter, tResultClass);
	}

	@Override
	public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Class<TResult> tResultClass) {
		return collection.distinct(clientSession, fieldName, tResultClass);
	}

	@Override
	public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<TResult> tResultClass) {
		return collection.distinct(clientSession, fieldName, filter, tResultClass);
	}

	@Override
	public RelayFindIterable<TDocument, TDocument> find() {
		return new RelayFindIterable<>(this, collection.find(), getDocumentClass());
	}

	@Override
	public <TResult> RelayFindIterable<TDocument, TResult> find(Class<TResult> tResultClass) {
		return new RelayFindIterable<>(this, collection.find(tResultClass), tResultClass);
	}

	@Override
	public RelayFindIterable<TDocument, TDocument> find(Bson filter) {
		return this.find().filter(filter);
	}

	@Override
	public <TResult> RelayFindIterable<TDocument, TResult> find(Bson filter, Class<TResult> tResultClass) {
		return this.find(tResultClass).filter(filter);
	}

	@Override
	public RelayFindIterable<TDocument, TDocument> find(ClientSession clientSession) {
		return new RelayFindIterable<>(this, collection.find(clientSession), getDocumentClass());
	}

	@Override
	public <TResult> RelayFindIterable<TDocument, TResult> find(ClientSession clientSession, Class<TResult> tResultClass) {
		return new RelayFindIterable<>(this, collection.find(clientSession, tResultClass), tResultClass);
	}

	@Override
	public RelayFindIterable<TDocument, TDocument> find(ClientSession clientSession, Bson filter) {
		return this.find(clientSession).filter(filter);
	}

	@Override
	public <TResult> RelayFindIterable<TDocument, TResult> find(ClientSession clientSession, Bson filter, Class<TResult> tResultClass) {
		return this.find(clientSession, tResultClass).filter(filter);
	}

	@Override
	public RelayAggregation<TDocument, TDocument> aggregate(List<? extends Bson> pipeline) {
		return new RelayAggregation<>(this, collection.aggregate(pipeline), getDocumentClass());
	}

	@Override
	public <TResult> RelayAggregation<TDocument, TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
		return new RelayAggregation<>(this, collection.aggregate(pipeline, tResultClass), tResultClass);
	}

	@Override
	public RelayAggregation<TDocument, TDocument> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
		return new RelayAggregation<>(this, collection.aggregate(clientSession, pipeline), getDocumentClass());
	}

	@Override
	public <TResult> RelayAggregation<TDocument, TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
		return new RelayAggregation<>(this, collection.aggregate(clientSession, pipeline, tResultClass), tResultClass);
	}

	@Override
	public ChangeStreamIterable<TDocument> watch() {
		return collection.watch();
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> tResultClass) {
		return collection.watch(tResultClass);
	}

	@Override
	public ChangeStreamIterable<TDocument> watch(List<? extends Bson> pipeline) {
		return collection.watch(pipeline);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
		return collection.watch(pipeline, tResultClass);
	}

	@Override
	public ChangeStreamIterable<TDocument> watch(ClientSession clientSession) {
		return collection.watch(clientSession);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> tResultClass) {
		return collection.watch(clientSession, tResultClass);
	}

	@Override
	public ChangeStreamIterable<TDocument> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
		return collection.watch(clientSession, pipeline);
	}

	@Override
	public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
		return collection.watch(clientSession, pipeline, tResultClass);
	}

	@Override
	public MapReduceIterable<TDocument> mapReduce(String mapFunction, String reduceFunction) {
		return collection.mapReduce(mapFunction, reduceFunction);
	}

	@Override
	public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
		return collection.mapReduce(mapFunction, reduceFunction, tResultClass);
	}

	@Override
	public MapReduceIterable<TDocument> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
		return collection.mapReduce(clientSession, mapFunction, reduceFunction);
	}

	@Override
	public <TResult> MapReduceIterable<TResult> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
		return collection.mapReduce(clientSession, mapFunction, reduceFunction, tResultClass);
	}

	@Override
	public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests) {
		return collection.bulkWrite(requests);
	}

	@Override
	public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
		return collection.bulkWrite(requests, options);
	}

	@Override
	public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests) {
		return collection.bulkWrite(clientSession, requests);
	}

	@Override
	public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
		return collection.bulkWrite(clientSession, requests, options);
	}

	@Override
	public InsertOneResult insertOne(TDocument tDocument) {
		return collection.insertOne(tDocument);
	}

	@Override
	public InsertOneResult insertOne(TDocument tDocument, InsertOneOptions options) {
		return collection.insertOne(tDocument, options);
	}

	@Override
	public InsertOneResult insertOne(ClientSession clientSession, TDocument tDocument) {
		return collection.insertOne(clientSession, tDocument);
	}

	@Override
	public InsertOneResult insertOne(ClientSession clientSession, TDocument tDocument, InsertOneOptions options) {
		return collection.insertOne(clientSession, tDocument, options);
	}

	@Override
	public InsertManyResult insertMany(List<? extends TDocument> tDocuments) {
		return collection.insertMany(tDocuments);
	}

	@Override
	public InsertManyResult insertMany(List<? extends TDocument> tDocuments, InsertManyOptions options) {
		return collection.insertMany(tDocuments, options);
	}

	@Override
	public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments) {
		return collection.insertMany(clientSession, tDocuments);
	}

	@Override
	public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments, InsertManyOptions options) {
		return collection.insertMany(clientSession, tDocuments, options);
	}

	@Override
	public DeleteResult deleteOne(Bson filter) {
		return collection.deleteOne(filter);
	}

	@Override
	public DeleteResult deleteOne(Bson filter, DeleteOptions options) {
		return collection.deleteOne(filter, options);
	}

	@Override
	public DeleteResult deleteOne(ClientSession clientSession, Bson filter) {
		return collection.deleteOne(clientSession, filter);
	}

	@Override
	public DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
		return collection.deleteOne(clientSession, filter, options);
	}

	@Override
	public DeleteResult deleteMany(Bson filter) {
		return collection.deleteMany(filter);
	}

	@Override
	public DeleteResult deleteMany(Bson filter, DeleteOptions options) {
		return collection.deleteMany(filter, options);
	}

	@Override
	public DeleteResult deleteMany(ClientSession clientSession, Bson filter) {
		return collection.deleteMany(clientSession, filter);
	}

	@Override
	public DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
		return collection.deleteMany(clientSession, filter, options);
	}

	@Override
	public UpdateResult replaceOne(Bson filter, TDocument replacement) {
		return collection.replaceOne(filter, replacement);
	}

	@Override
	public UpdateResult replaceOne(Bson filter, TDocument replacement, ReplaceOptions updateOptions) {
		return collection.replaceOne(filter, replacement, updateOptions);
	}

	@Override
	public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement) {
		return collection.replaceOne(clientSession, filter, replacement);
	}

	@Override
	public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement, ReplaceOptions updateOptions) {
		return collection.replaceOne(clientSession, filter, replacement, updateOptions);
	}

	@Override
	public UpdateResult updateOne(Bson filter, Bson update) {
		return collection.updateOne(filter, update);
	}

	@Override
	public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
		return collection.updateOne(filter, update, updateOptions);
	}

	@Override
	public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update) {
		return collection.updateOne(clientSession, filter, update);
	}

	@Override
	public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
		return collection.updateOne(clientSession, filter, update, updateOptions);
	}

	@Override
	public UpdateResult updateOne(Bson bson, List<? extends Bson> list) {
		return collection.updateOne(bson, list);
	}

	@Override
	public UpdateResult updateOne(Bson bson, List<? extends Bson> list, UpdateOptions updateOptions) {
		return collection.updateOne(bson, list, updateOptions);
	}

	@Override
	public UpdateResult updateOne(ClientSession clientSession, Bson bson, List<? extends Bson> list) {
		return collection.updateOne(clientSession, bson, list);
	}

	@Override
	public UpdateResult updateOne(ClientSession clientSession, Bson bson, List<? extends Bson> list, UpdateOptions updateOptions) {
		return collection.updateOne(clientSession, bson, list, updateOptions);
	}

	@Override
	public UpdateResult updateMany(Bson filter, Bson update) {
		return collection.updateMany(filter, update);
	}

	@Override
	public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
		return collection.updateMany(filter, update, updateOptions);
	}

	@Override
	public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update) {
		return collection.updateMany(clientSession, filter, update);
	}

	@Override
	public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
		return collection.updateMany(clientSession, filter, update, updateOptions);
	}

	@Override
	public UpdateResult updateMany(Bson bson, List<? extends Bson> list) {
		return collection.updateMany(bson, list);
	}

	@Override
	public UpdateResult updateMany(Bson bson, List<? extends Bson> list, UpdateOptions updateOptions) {
		return collection.updateMany(bson, list, updateOptions);
	}

	@Override
	public UpdateResult updateMany(ClientSession clientSession, Bson bson, List<? extends Bson> list) {
		return collection.updateMany(clientSession, bson, list);
	}

	@Override
	public UpdateResult updateMany(ClientSession clientSession, Bson bson, List<? extends Bson> list, UpdateOptions updateOptions) {
		return collection.updateMany(clientSession, bson, list, updateOptions);
	}

	@Override
	public TDocument findOneAndDelete(Bson filter) {
		return collection.findOneAndDelete(filter);
	}

	@Override
	public TDocument findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
		return collection.findOneAndDelete(filter, options);
	}

	@Override
	public TDocument findOneAndDelete(ClientSession clientSession, Bson filter) {
		return collection.findOneAndDelete(clientSession, filter);
	}

	@Override
	public TDocument findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
		return collection.findOneAndDelete(clientSession, filter, options);
	}

	@Override
	public TDocument findOneAndReplace(Bson filter, TDocument replacement) {
		return collection.findOneAndReplace(filter, replacement);
	}

	@Override
	public TDocument findOneAndReplace(Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
		return collection.findOneAndReplace(filter, replacement, options);
	}

	@Override
	public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement) {
		return collection.findOneAndReplace(clientSession, filter, replacement);
	}

	@Override
	public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
		return collection.findOneAndReplace(clientSession, filter, replacement, options);
	}

	@Override
	public TDocument findOneAndUpdate(Bson filter, Bson update) {
		return collection.findOneAndUpdate(filter, update);
	}

	@Override
	public TDocument findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
		return collection.findOneAndUpdate(filter, update, options);
	}

	@Override
	public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
		return collection.findOneAndUpdate(clientSession, filter, update);
	}

	@Override
	public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update, FindOneAndUpdateOptions options) {
		return collection.findOneAndUpdate(clientSession, filter, update, options);
	}

	@Override
	public TDocument findOneAndUpdate(Bson bson, List<? extends Bson> list) {
		return collection.findOneAndUpdate(bson, list);
	}

	@Override
	public TDocument findOneAndUpdate(Bson bson, List<? extends Bson> list, FindOneAndUpdateOptions findOneAndUpdateOptions) {
		return collection.findOneAndUpdate(bson, list, findOneAndUpdateOptions);
	}

	@Override
	public TDocument findOneAndUpdate(ClientSession clientSession, Bson bson, List<? extends Bson> list) {
		return collection.findOneAndUpdate(clientSession, bson, list);
	}

	@Override
	public TDocument findOneAndUpdate(ClientSession clientSession, Bson bson, List<? extends Bson> list, FindOneAndUpdateOptions findOneAndUpdateOptions) {
		return collection.findOneAndUpdate(clientSession, bson, list, findOneAndUpdateOptions);
	}

	@Override
	public void drop() {
		collection.drop();
	}

	@Override
	public void drop(ClientSession clientSession) {
		collection.drop(clientSession);
	}

	@Override
	public void drop(DropCollectionOptions dropCollectionOptions) {
		collection.drop(dropCollectionOptions);
	}

	@Override
	public void drop(ClientSession clientSession, DropCollectionOptions dropCollectionOptions) {
		collection.drop(clientSession, dropCollectionOptions);
	}

	@Override
	public String createIndex(Bson keys) {
		return collection.createIndex(keys);
	}

	@Override
	public String createIndex(Bson keys, IndexOptions indexOptions) {
		return collection.createIndex(keys, indexOptions);
	}

	@Override
	public String createIndex(ClientSession clientSession, Bson keys) {
		return collection.createIndex(clientSession, keys);
	}

	@Override
	public String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions) {
		return collection.createIndex(clientSession, keys, indexOptions);
	}

	@Override
	public List<String> createIndexes(List<IndexModel> indexes) {
		return collection.createIndexes(indexes);
	}

	@Override
	public List<String> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
		return collection.createIndexes(indexes, createIndexOptions);
	}

	@Override
	public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
		return collection.createIndexes(clientSession, indexes);
	}

	@Override
	public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
		return collection.createIndexes(clientSession, indexes, createIndexOptions);
	}

	@Override
	public ListIndexesIterable<Document> listIndexes() {
		return collection.listIndexes();
	}

	@Override
	public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> tResultClass) {
		return collection.listIndexes(tResultClass);
	}

	@Override
	public ListIndexesIterable<Document> listIndexes(ClientSession clientSession) {
		return collection.listIndexes(clientSession);
	}

	@Override
	public <TResult> ListIndexesIterable<TResult> listIndexes(ClientSession clientSession, Class<TResult> tResultClass) {
		return collection.listIndexes(clientSession, tResultClass);
	}

	@Override
	public void dropIndex(String indexName) {
		collection.dropIndex(indexName);
	}

	@Override
	public void dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
		collection.dropIndex(indexName, dropIndexOptions);
	}

	@Override
	public void dropIndex(Bson keys) {
		collection.dropIndex(keys);
	}

	@Override
	public void dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
		collection.dropIndex(keys, dropIndexOptions);
	}

	@Override
	public void dropIndex(ClientSession clientSession, String indexName) {
		collection.dropIndex(clientSession, indexName);
	}

	@Override
	public void dropIndex(ClientSession clientSession, Bson keys) {
		collection.dropIndex(clientSession, keys);
	}

	@Override
	public void dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
		collection.dropIndex(clientSession, indexName, dropIndexOptions);
	}

	@Override
	public void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
		collection.dropIndex(clientSession, keys, dropIndexOptions);
	}

	@Override
	public void dropIndexes() {
		collection.dropIndexes();
	}

	@Override
	public void dropIndexes(ClientSession clientSession) {
		collection.dropIndexes(clientSession);
	}

	@Override
	public void dropIndexes(DropIndexOptions dropIndexOptions) {
		collection.dropIndexes(dropIndexOptions);
	}

	@Override
	public void dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
		collection.dropIndexes(clientSession, dropIndexOptions);
	}

	@Override
	public void renameCollection(MongoNamespace newCollectionNamespace) {
		collection.renameCollection(newCollectionNamespace);
	}

	@Override
	public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
		collection.renameCollection(newCollectionNamespace, renameCollectionOptions);
	}

	@Override
	public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
		collection.renameCollection(clientSession, newCollectionNamespace);
	}

	@Override
	public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
		collection.renameCollection(clientSession, newCollectionNamespace, renameCollectionOptions);
	}

	/**
	 * Inserts or update items based on defined access control
	 * @param item
	 * @param executor
	 * @return
	 */
	public CompletableFuture<TDocument> insertOrUpdate(TDocument item, Executor executor) {
		return this.insertOrUpdate(item, null, executor);
	}

	/**
	 * Inserts or update items based on defined access control for the given filter
	 * @param item
	 * @param filter
	 * @param executor
	 * @return
	 */
	public CompletableFuture<TDocument> insertOrUpdate(TDocument item, Bson filter, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.insertOrUpdate(item, filter);
			} catch (RelayException e) {
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Inserts or update items based on defined access control
	 * @param item
	 * @return
	 */
	public TDocument insertOrUpdate(TDocument item) throws RelayException {
		return this.insertOrUpdate(item, (Bson) null);
	}

	/**
	 * Inserts or update items based on defined access control and the given filter
	 * @param item
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public TDocument insertOrUpdate(TDocument item, Bson filter) throws RelayException {
		try {
			this.validate(item, filter);

			if (item instanceof Document) {
				return (TDocument) this.insertOrUpdate((Document) item);
			}

			if (item instanceof RelayModel) {
				return (TDocument) this.insertOrUpdate((RelayModel) item);
			}

			throw new RelayException(Http.Status.BAD_REQUEST, "invalid_parameters");
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}

	/**
	 * Inserts a mongo document
	 * @param item
	 * @return
	 * @throws RelayException
	 */
	@SuppressWarnings("unchecked")
	private Document insertOrUpdate(Document item) throws RelayException {
		try {
			if (item.getObjectId("_id") != null) {
				item.append("updatedAt", new Date().getTime());
				UpdateResult result = collection.replaceOne(new BasicDBObject("_id", item.getObjectId("_id")), (TDocument) item);
				if (result.wasAcknowledged() && result.getModifiedCount() > 0) {
					return item;
				}
				throw new RelayException(Http.Status.NOT_FOUND, "not_found");
			}
			collection.insertOne((TDocument) item);
			return item;
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}

	/**
	 * Inserts a Mongo Collection Model item
	 * @param item
	 * @return
	 * @throws RelayException
	 */
	@SuppressWarnings("unchecked")
	private RelayModel insertOrUpdate(RelayModel item) throws RelayException {
		try {
			if (item.getId() != null) {
				item.setUpdatedAt(new Date().getTime());
				UpdateResult result = this.replaceOne(new BasicDBObject("_id", item.getId()), (TDocument) item);
				if (result.wasAcknowledged() && result.getModifiedCount() > 0) {
					return item;
				}
				throw new RelayException(404, "not_found");
			}
			item.setId(new ObjectId());
			this.insertOne((TDocument) item);
			return item;
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}

	/**
	 * Deletes a document from mongo, given the access control
	 * @param item
	 * @return
	 */
	public CompletableFuture<TDocument> deleteItem(TDocument item, Executor executor) {
		return this.deleteItem(item, null, executor);
	}

	/**
	 * Deletes a document from mongo, given the access control and filtering
	 * @param item
	 * @return
	 */
	public CompletableFuture<TDocument> deleteItem(TDocument item, Bson filter, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.deleteItem(item, filter);
			} catch (RelayException e) {
				e.printStackTrace();
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Deletes an item based on access control
	 * @param item
	 * @return
	 */
	public TDocument deleteItem(TDocument item) throws RelayException {
		return this.deleteItem(item, (Bson) null);
	}

	/**
	 * Deletes an item based on access control and filtering
	 * @param item
	 * @return
	 */
	public <TDocument> TDocument deleteItem(TDocument item, Bson filter) throws RelayException {
		try {
			this.validate(item, filter, false);

			ObjectId id = null;
			if (item instanceof Document) {
				id = ((Document) item).getObjectId("_id");
			}

			if (item instanceof RelayModel) {
				id = ((RelayModel) item).getId();
			}

			DeleteResult result = this.deleteOne(new BasicDBObject("_id", id));
			if (result.wasAcknowledged() && result.getDeletedCount() > 0) {
				return item;
			}
			// if no objcetid return bad request else not found
			if (id == null) {
				throw new RelayException(Http.Status.BAD_REQUEST, "invalid_parameters");
			}
			throw new RelayException(Http.Status.NOT_FOUND, "not_found", List.of(id));
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}


	/**
	 * Deletes an object async based on access control
	 * @param item
	 * @return
	 */
	public CompletableFuture<ObjectId> deleteItem(ObjectId item, Executor executor) {
		return this.deleteItem(item, null, executor);
	}

	/**
	 * Deletes an object async based on access control and filtering
	 *
	 * @param item
	 * @return
	 */
	public CompletableFuture<ObjectId> deleteItem(ObjectId item, Bson filter, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.deleteItem(item, filter);
			} catch (RelayException e) {
				e.printStackTrace();
				throw new CompletionException(e);
			}
		}, executor);
	}

	/**
	 * Deletes an item based on access control
	 * @param id
	 * @return
	 */
	public ObjectId deleteItem(ObjectId id, Bson filter) throws RelayException {
		try {
			// if no object-id return bad request else not found
			if (id == null) {
				throw new RelayException(Http.Status.BAD_REQUEST, "invalid_parameters");
			}
			this.validate(id, filter, false);
			DeleteResult result = this.deleteOne(new BasicDBObject("_id", id));
			if (result.wasAcknowledged() && result.getDeletedCount() > 0) {
				return id;
			}
			throw new RelayException(Http.Status.NOT_FOUND, "not_found", List.of(id));
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param id
	 * @param context
	 * @return
	 */
	public CompletableFuture<TDocument> byId(ObjectId id, Executor context) {
		return this.byId(id, null, context);
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param key
	 * @param context
	 * @return
	 */
	public CompletableFuture<TDocument> byKey(Bson key, Executor context) {
		return this.byKey(key, null, context);
	}

	/**
	 * Retrieves a document on mongo, access control is checked based on filter
	 * @param id
	 * @param filter
	 * @param context
	 * @return
	 */
	public CompletableFuture<TDocument> byId(ObjectId id, Bson filter, Executor context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.byId(id, filter);
			} catch (RelayException e) {
				throw new CompletionException(e);
			}
		}, context);
	}

	/**
	 * Retrieves a document on mongo, access control is checked based on filter
	 * @param key
	 * @param filter
	 * @param context
	 * @return
	 */
	public CompletableFuture<TDocument> byKey(Bson key, Bson filter, Executor context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return this.byKey(key, filter);
			} catch (RelayException e) {
				throw new CompletionException(e);
			}
		}, context);
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param id
	 * @return
	 * @throws RelayException
	 */
	public TDocument byId(ObjectId id) throws RelayException {
		return this.byId(id, (Bson) null);
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param key
	 * @return
	 * @throws RelayException
	 */
	public TDocument byKey(Bson key) throws RelayException {
		return this.byKey(key, (Bson) null);
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param id
	 * @param filter
	 * @return
	 * @throws RelayException
	 */
	public TDocument byId(ObjectId id, Bson filter) throws RelayException {
		try {
			TDocument found = this.validate(id, filter);
			if (found == null) {
				// not found
				throw new RelayException(Http.Status.NOT_FOUND, "not_found", List.of(id));
			}
			return found;
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}

	/**
	 * Retrieves a document on mongo, access control is checked
	 * @param key
	 * @param filter
	 * @return
	 * @throws RelayException
	 */
	public TDocument byKey(Bson key, Bson filter) throws RelayException {
		try {
			TDocument found = this.validate(key, filter);
			if (found == null) {
				// not found
				throw new RelayException(Http.Status.NOT_FOUND, "not_found");
			}
			return found;
		} catch (RelayException ex) {
			throw ex;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RelayException(Http.Status.INTERNAL_SERVER_ERROR, "service_unavailable");
		}
	}
}
