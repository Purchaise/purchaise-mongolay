package io.purchaise.mongolay;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.purchaise.mongolay.annotations.Index;
import io.purchaise.mongolay.annotations.Reference;
import io.purchaise.mongolay.annotations.SubClasses;
import io.purchaise.mongolay.references.IReference;
import io.purchaise.mongolay.references.InnerReferenceField;
import io.purchaise.mongolay.references.ReferencedField;
import io.purchaise.mongolay.utils.ClassUtils;
import io.purchaise.mongolay.utils.HibernateValidator;
import io.purchaise.mongolay.utils.IndexType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by agonlohaj on 07 Aug, 2019
 */
@AllArgsConstructor
public class MongoRelay {
	/**
	 * Simple Mongo Database
	 */
	@Getter
	@Setter
	MongoDatabase database;

	protected String packageName = "io.goprime.mongolay";

	/**
	 * Depth
	 */
	@Getter
	private int depth = 0;

	/**
	 * Max Reference Depth
	 */
	@Getter
	private int maxDepth = 1;
	@Getter
	private Map<Class<?>, String> collections = new HashMap<>();
	@Getter
	private Map<String, Object> referenceFields = new HashMap<>();
	@Getter
	private Map<Class, Integer> classMaxDepth = new HashMap<>();

	private List<String> roles = new ArrayList<>();

	Map<Class, AccessLevelType> collectionClassAcl = new HashMap<>();
	Map<String, AccessLevelType> collectionNameAcl = new HashMap<>();

	public MongoRelay() {}

	/**
	 * Constructs a new Mongo Relay, gfor a given Mongo Database
	 * @param database
	 */
	public MongoRelay (MongoDatabase database) {
		this.database = database;
	}

	/**
	 * Constructs a new Mongo Relay, gfor a given Mongo Database
	 * @param database
	 */
	public MongoRelay (MongoDatabase database, List<String> roles) {
		this.database = database;
		this.roles = roles;
	}

	/**
	 * Constructs a new Mongo Relay, given the user, has access to user database, and can enforce ACL
	 * @param copy
	 */
	public MongoRelay (MongoRelay copy) {
		this.database = copy.database;
		this.depth = copy.depth;
		this.maxDepth = copy.maxDepth;
		this.collectionClassAcl = copy.collectionClassAcl;
		this.collectionNameAcl = copy.collectionNameAcl;
		this.classMaxDepth = copy.classMaxDepth;
		this.collections = copy.collections;
		this.referenceFields = copy.referenceFields;
	}

	public MongoRelay copy () {
		return new MongoRelay(this);
	}

	public MongoRelay withDepth (int depth) {
		this.depth = depth;
		return this;
	}

	public MongoRelay withMaxDepth (int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	public MongoRelay withCollections(Class<?> clazz, String collection) {
		this.collections.put(clazz, collection);
		return this;
	}

	public MongoRelay withCollections(Map<Class<?>, String> collections) {
		this.collections.putAll(collections);
		return this;
	}

	public MongoRelay withReferenceFields(String field, Object value) {
		this.referenceFields.put(field, value);
		return this;
	}

	public MongoRelay withReferenceFields(Map<String, Object> references) {
		this.referenceFields.putAll(references);
		return this;
	}

	public MongoRelay withClassMaxDepth (Class clazz, int maxDepth) {
		classMaxDepth.put(clazz, maxDepth);
		return this;
	}

	public MongoRelay withACL (Class clazz, AccessLevelType type) {
		collectionClassAcl.put(clazz, type);
		return this;
	}

	public MongoRelay withACL (String collectionName, AccessLevelType type) {
		collectionNameAcl.put(collectionName, type);
		return this;
	}

	protected <T> T validate (T value, RelayCollection<T> collection) throws RelayException {
		return this.validate(value, collection, null);
	}

	protected <T> T validate (T value, RelayCollection<T> collection, Bson filter) throws RelayException {
		return this.validate(value, collection, filter, true);
	}

	protected <T> T validate (T value, RelayCollection<T> collection, Bson filter, boolean withModel) throws RelayException {
		// perform a Hibernate Check
		if (withModel) {
			HibernateValidator.validate(value);
		}
		// check if its with read or write ACL
		ObjectId id = null;
		// I can only check access for Mongo Collection Model
		if (value instanceof RelayModel) {
			RelayModel model = (RelayModel) value;
			id = model.getId();
		}
		// or Document model
		if (value instanceof Document) {
			Document model = (Document) value;
			id = model.getObjectId("_id");
		}
		return this.validate(id, collection, filter);
	}

	@SuppressWarnings("unchecked")
	protected <T> T validate (ObjectId value, RelayCollection<T> collection, Bson filter) throws RelayException {
		return this.validate(Filters.eq("_id", value), collection, filter);
	}

	@SuppressWarnings("unchecked")
	protected <T> T validate (Bson key, RelayCollection<T> collection, Bson filter) throws RelayException {
		// check if its with read or write ACL
		Class<T> clazz = collection.getDatabase().getClazz();
		AccessControl accessControl = this.accessControl(collection.getDocumentClass(), collection.getDatabase().getCollectionName());

		if (accessControl == null) {
			return collection.find(key).first();
		}
		// I can only check access for Mongo Collection Model
		if (RelayModel.class.isAssignableFrom(clazz)) {
			return (T) accessControl.isAccessibleOnCollection((RelayCollection<RelayModel>) collection, filter, key);
		}
		// or Document model
		if (Document.class.isAssignableFrom(clazz)) {
			return (T) accessControl.isAccessible((RelayCollection<Document>) collection, filter, key);
		}

		return null;
	}

	/**
	 * If the class has entity defined, or its simple class name is the same as the mongo collection name, than use this!
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public <T> RelayDatabase<T> on(Class<T> clazz) {
		String collectionName = collections.getOrDefault(clazz, ClassUtils.entityName(clazz));
		return new RelayDatabase<T>(this, this.mongoDatabaseOn(clazz))
			.withClass(clazz)
			.withCollectionName(collectionName);
	}

	/**
	 * Using this method, you won't benefit out of internal mappings, instead this would be you're normal
	 * Mongo Document class for which this library cannot add mappings or ACL
	 * @param collectionName
	 * @return
	 */
	@Deprecated
	public RelayDatabase<Document> on(String collectionName) {
		return new RelayDatabase<Document>(this, this.mongoDatabaseOn(collectionName)).withClass(Document.class).withCollectionName(collectionName);
	}

	/**
	 * Given the initial collection type, get the mongo-database with a different result class,
	 * Comes in handy when you start your queries in a class and end up in a different class from the source
	 * Aggregations will use this often
	 * @param collectionClass
	 * @param resultClass
	 * @param <T>
	 * @param <C>
	 * @return
	 */
	public <T, C> RelayDatabase<T> on(Class<C> collectionClass, Class<T> resultClass) {
		return new RelayDatabase<T>(this, this.mongoDatabaseOn(collectionClass)).withClass(resultClass).withCollectionName(ClassUtils.entityName(collectionClass));
	}

	/**
	 * Given the initial collection name, get the mongo-database with a different result class,
	 * Comes in handy when you start your queries in a class and end up in a different class from the source
	 * Aggregations will use this often
	 * @param collectionName
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public <T> RelayDatabase<T> on(String collectionName, Class<T> clazz) {
		return new RelayDatabase<T>(this, this.mongoDatabaseOn(collectionName)).withClass(clazz).withCollectionName(collectionName);
	}

	public <T> List<Bson> acl (Class<T> clazz, String collectionName) {
		AccessControl control = this.accessControl(clazz, collectionName);
		if (control == null) {
			return new ArrayList<>();
		}
		return control.applyAccessFilter();
	}

	/**
	 * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
	 * @param item
	 * @param <TResult>
	 * @return
	 */
	public  <A extends Collection<? super TResult>, TResult, TDocument> A map(A item, Class<TDocument> clazz) {
		return this.map(item, this.discover(clazz), clazz);
	}

	/**
	 * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
	 * @param item
	 * @return
	 */
	public <TResult, TDocument> TResult map(TResult item, Class<TDocument> clazz) {
		return this.map(Arrays.asList(item), this.discover(clazz), clazz).get(0);
	}

	protected <T> AccessControl accessControl (Class<T> clazz, String collectionName) {
		AccessLevelType type = this.aclTypeOn(clazz, collectionName);
		if (type == null || type == AccessLevelType.NONE) {
			return null;
		}
		if (roles.size() == 0) {
			return null;
		}
		return new AccessControl(roles, type);
	}

	protected <T> AccessLevelType aclTypeOn (Class<T> clazz, String collectionName) {
		AccessLevelType type = collectionClassAcl.get(clazz);
		if (type != null) {
			return type;
		}
		AccessLevelType entity = this.aclTypeOn(ClassUtils.entityName(clazz));
		if (entity != null) {
			return entity;
		}
		return this.aclTypeOn(collectionName);
	}

	protected AccessLevelType aclTypeOn (String collectionName) {
		if (collectionName == null || collectionName.isBlank()) {
			return null;
		}
		return collectionNameAcl.get(collectionName);
	}

	protected <T> MongoDatabase mongoDatabaseOn(Class<T> clazz) {
		return this.database;
	}

	protected MongoDatabase mongoDatabaseOn(String collectionName) {
		return this.database;
	}

	/**
	 * The core of the Mongo Relay Library, does the mapping based on annotations at targeted value
	 * @param item
	 * @param references
	 * @return mapped collection
	 */
	private <A extends Collection<? super TResult>, TResult, TDocument> A map(A item, List<IReference> references, Class<TDocument> clazz) {
		if (depth >= maxDepth) {
			return item;
		}
		// now also check if class depth exceeds max depth
		if (depth >= classMaxDepth.getOrDefault(clazz, maxDepth)) {
			return item;
		}

		if (references.size() == 0) {
			return item;
		}

		MongoRelay relay = this.copy();
		// increase the depth by one, such that on next mapping it knows how deep it went
		relay.depth += 1;

		// Now that we have the reference mapping, lets form the filter which will get the references from Mongo and assign the values back to the target
		// That's apply the referencing
		for (IReference reference: references) {
			reference.map(item, relay);
		}

		return item;
	}

	private <T> List<IReference> discover (Class<T> clazz) {
		List<Field> referencedFields = FieldUtils.getFieldsListWithAnnotation(clazz, Reference.class);
		List<IReference> references = referencedFields
				.stream()
				.map(next -> discover(next, clazz))
				.filter(IReference::isValid)
				.collect(Collectors.toList());

		SubClasses subClasses = clazz.getDeclaredAnnotation(SubClasses.class);
		if (subClasses != null) {
			List<IReference> subs = Arrays.stream(subClasses.of())
					.map(this::discover)
					.flatMap(Collection::stream)
					.collect(Collectors.toList());
			references.addAll(subs);
		}
		return references;
	}

	private <T> IReference discover (Field next, Class<T> clazz) {
		Reference annotation = next.getAnnotation(Reference.class);
		if (!annotation.nested()) {
			ReferencedField referencedField = new ReferencedField(clazz, next, annotation.projections(), new ArrayList<>(), annotation.collection());
			String[] from = annotation.from();
			String[] to = annotation.to();
			for (int i = 0; i < from.length; i++) {
				String source = from[i];
				String target = to[i];
				referencedField.addReference(new FieldReference(source, target));
			}
			return referencedField;
		}
		return new InnerReferenceField(clazz, next, annotation.subclasses());
	}

	public void ensureIndexes (String directory) {
		Reflections reflections = new Reflections(directory);
		Set<Class<? extends RelayModel>> annotated = reflections.getSubTypesOf(RelayModel.class);

		for (Class<? extends RelayModel> interceptor: annotated) {
			List<Field> referencedFields = FieldUtils.getFieldsListWithAnnotation(interceptor, Index.class);

			referencedFields.forEach(field -> ensureIndexes(field, interceptor));
		}
	}

	public void ensureIndexes (Class<? extends RelayModel> clazz) {
		List<Field> referencedFields = FieldUtils.getFieldsListWithAnnotation(clazz, Index.class);
		referencedFields.forEach(field -> ensureIndexes(field, clazz));
	}

	private void ensureIndexes (Field field, Class<? extends RelayModel> clazz) {
		Index annotation = field.getAnnotation(Index.class);
		String name = FieldReference.getMongoClass(field);
		RelayCollection collection = on(clazz).getCollection();
		IndexType type = annotation.type();
		boolean background = annotation.background();
		try {
			switch (type) {
				case DESC:
					collection.createIndex(Indexes.descending(name), new IndexOptions().background(background));
					break;
				case ASC:
					collection.createIndex(Indexes.ascending(name), new IndexOptions().background(background));
					break;
				case TEXT:
					collection.createIndex(Indexes.text(name), new IndexOptions().background(background));
					break;
				case VECTOR_SEARCH:
					Document vectorField = new Document("type", "vector")
							.append("numDimensions", 1536)
							.append("path", name)
							.append("similarity", "cosine");
					String[] filters = annotation.filters();
					List<Document> fields = new ArrayList<>();
					fields.add(vectorField);
					for (String filter: filters) {
						fields.add(new Document("path", filter).append("type", "filter"));
					}
					collection.createSearchIndex(String.format("%s_vector_index", name), new Document("fields", fields));
					break;
			}
		}  catch (Exception ex) {
			System.out.println("Error creating index: " + ex.getMessage());
		}
	}
}
