package io.purchaise.mongolay;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.purchaise.mongolay.annotations.*;
import io.purchaise.mongolay.annotations.atlasSearch.*;
import io.purchaise.mongolay.annotations.atlasSearch.customAnalyzers.*;
import io.purchaise.mongolay.options.IOption;
import io.purchaise.mongolay.options.enums.OptionType;
import io.purchaise.mongolay.references.IReference;
import io.purchaise.mongolay.references.InnerReferenceField;
import io.purchaise.mongolay.references.ReferencedField;
import io.purchaise.mongolay.utils.ClassUtils;
import io.purchaise.mongolay.utils.HibernateValidator;
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
import java.util.stream.Stream;

import static io.purchaise.mongolay.utils.FieldType.EMBEDDED_DOCUMENTS;

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

	protected String packageName = "io.purchaise.mongolay";

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

	/**
	 * Stores option configurations for different classes and option types, which are used on query/aggregation stages.
	 * The key is the class for which the options are defined, and the value is a map where the key is the option type
	 * and the value is the option configuration.
	 */
	@Getter
	private Map<Class<?>, Map<OptionType, IOption>> options = new HashMap<>();

	public MongoRelay() {}

	/**
	 * Constructs a new Mongo Relay for a given Mongo Database
	 * @param database
	 */
	public MongoRelay (MongoDatabase database) {
		this.database = database;
	}

	/**
	 * Constructs a new Mongo Relay, for a given Mongo Database
	 * @param database
	 */
	public MongoRelay (MongoDatabase database, List<String> roles) {
		this.database = database;
		this.roles = roles;
	}

	public MongoRelay (MongoDatabase database, Map<Class<?>, Map<OptionType, IOption>> options) {
		this.database = database;
		this.options = options;
	}

	public MongoRelay (MongoDatabase database, List<String> roles, Map<Class<?>, Map<OptionType, IOption>> options) {
		this.database = database;
		this.roles = roles;
		this.options = options;
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
		this.options = copy.options;
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

	public MongoRelay withOptions (Map<Class<?>, Map<OptionType, IOption>> options) {
		this.options.putAll(options);
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
		return new RelayDatabase<Document>(this, this.mongoDatabaseOn(collectionName))
				.withClass(Document.class)
				.withCollectionName(collectionName);
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
		String collectionName = collections.getOrDefault(collectionClass, ClassUtils.entityName(collectionClass));
		return new RelayDatabase<T>(this, this.mongoDatabaseOn(collectionClass))
				.withSourceClass(collectionClass)
				.withClass(resultClass)
				.withCollectionName(collectionName);
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
		return new RelayDatabase<T>(this, this.mongoDatabaseOn(collectionName))
				.withClass(clazz)
				.withCollectionName(collectionName);
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

	/**
	 * Ensures indexes for all annotated classes in the specified directory.
	 * Dynamic entities will have indexes created for each of their matching collections.
	 *
	 * @param directory the package directory to scan for classes extending RelayModel
	 */
	public void ensureIndexes(String directory) {
		Reflections reflections = new Reflections(directory);
		Set<Class<? extends RelayModel>> annotated = reflections.getSubTypesOf(RelayModel.class)
				.stream().filter((next) -> next.isAnnotationPresent(Entity.class))
				.filter(next -> !next.isAnonymousClass())
				.collect(Collectors.toSet());

		List<String> allCollections = annotated
				.stream()
				.map(next -> this.collections.getOrDefault(next, ClassUtils.entityName(next)))
				.collect(Collectors.toList());

		for (Class<? extends RelayModel> interceptor : annotated) {
			String collectionName = this.collections.getOrDefault(interceptor, ClassUtils.entityName(interceptor));
			List<String> collectionNames = Collections.singletonList(collectionName);
			if (ClassUtils.isDynamicEntity(interceptor)) {
				collectionNames = this.getDatabase()
						.listCollectionNames()
						.into(new ArrayList<>())
						.stream()
						.filter(name -> name.startsWith(collectionName) &&
								allCollections.stream().noneMatch(next -> !next.equals(collectionName) && next.startsWith(name))
						)
						.collect(Collectors.toList());
			}

			collectionNames.forEach(name -> this.ensureIndexes(interceptor, name));
		}
	}

	/**
	 * Ensures indexes for all fields annotated with @Index for the given class.
	 *
	 * @param clazz the class to process
	 */
	public void ensureIndexes(Class<? extends RelayModel> clazz) {
		this.ensureIndexes(clazz, collections.getOrDefault(clazz, ClassUtils.entityName(clazz)));
	}

	/**
	 * Ensures indexes for all fields annotated with @Index for the given class and collection.
	 *
	 * @param clazz          the class to process
	 * @param collectionName the name of the collection
	 */
	public void ensureIndexes(Class<? extends RelayModel> clazz, String collectionName) {
		List<Field> indexedFields = FieldUtils.getFieldsListWithAnnotation(clazz, Index.class);
		indexedFields.forEach(field -> this.ensureIndexes(field, collectionName));

		RelayCollection<Document> collection = this.on(collectionName).getCollection();
		List<Document> existing = collection.listSearchIndexes().into(new ArrayList<>());

		this.ensureCompoundIndexes(clazz, collectionName);
		this.ensureAtlasSearchIndexes(clazz, collectionName, existing);
		this.ensureSearchIndex(clazz, collectionName, existing);
	}

	/**
	 * Ensures an index for a specific field in the collection associated with the given class.
	 *
	 * @param field the field to index
	 * @param clazz the class containing the field
	 */
	private void ensureIndexes(Field field, Class<? extends RelayModel> clazz) {
		this.ensureIndexes(field, collections.getOrDefault(clazz, ClassUtils.entityName(clazz)));
	}

	/**
	 * Ensures an index for a specific field in the specified collection.
	 *
	 * @param field          the field to index
	 * @param collectionName the name of the collection
	 */
	private void ensureIndexes(Field field, String collectionName) {
		String fieldName = FieldReference.getMongoClass(field);
		Index annotation = field.getAnnotation(Index.class);
		RelayCollection<Document> collection = this.on(collectionName).getCollection();
		IndexOptions options = new IndexOptions().background(annotation.background());
		try {
			switch (annotation.type()) {
				case DESC:
					collection.createIndex(Indexes.descending(fieldName), options);
					break;
				case ASC:
					collection.createIndex(Indexes.ascending(fieldName), options);
					break;
				case TEXT:
					collection.createIndex(Indexes.text(fieldName), options);
					break;
			}
		} catch (Exception exception) {
			System.err.println("Error creating index for field " + fieldName + " in collection " + collectionName + ": " + exception.getMessage());
		}
	}

	/**
	 * Ensure atlas vector search in particular
	 * @param clazz
	 * @param collectionName
	 */
	private void ensureAtlasSearchIndexes(Class<? extends RelayModel> clazz, String collectionName, List<Document> existing) {
		List<Field> indexedFields = FieldUtils.getFieldsListWithAnnotation(clazz, VectorSearchIndex.class);

		List<SearchIndexModel> indexes = indexedFields
				.stream()
				.map(field -> {
					VectorSearchIndex annotation = field.getAnnotation(VectorSearchIndex.class);
					String name = FieldReference.getMongoClass(field);

					Document vectorField = new Document("type", "vector")
							.append("numDimensions", annotation.numOfDimensions())
							.append("path", name)
							.append("similarity", annotation.similarity());
					String[] filters = annotation.filters();
					List<Document> fields = new ArrayList<>();
					fields.add(vectorField);
					for (String filter: filters) {
						fields.add(new Document("path", filter).append("type", "filter"));
					}
					// Define the index model
					return new SearchIndexModel(
							String.format("%s_vector_index", name),
							new Document("fields", fields),
							SearchIndexType.vectorSearch()
					);
				})
				.filter((next) -> existing.stream().noneMatch((which) -> which.get("name", "").equals(next.getName())))
				.collect(Collectors.toList());
		if (indexes.isEmpty()) {
			return;
		}
		try {
			RelayCollection<Document> collection = this.on(collectionName).getCollection();
			collection.createSearchIndexes(indexes);
		} catch (MongoCommandException e) {
			if (e.getErrorCode() == 26) {
				// NamespaceNotFound
				// ignore
				return;
			}
			throw e;
		}
	}

	private void ensureCompoundIndexes(Class<? extends RelayModel> clazz, String collectionName) {

		List<CompoundIndex> compoundIndexes = List.of(clazz.getAnnotationsByType(CompoundIndex.class));
		List<IndexModel> indexes = compoundIndexes
				.stream()
				.map(item -> new IndexModel(Indexes.compoundIndex(
						Arrays.stream(item.indexes())
								.map(index ->
										index.type().equals(IndexType.ASC) ?
												Indexes.ascending(index.field()) :
												Indexes.descending(index.field())
								)
								.collect(Collectors.toList())
				)))
				.collect(Collectors.toList());
		if (indexes.isEmpty()) {
			return;
		}
		RelayCollection<Document> collection = this.on(collectionName).getCollection();
		try {
			collection.createIndexes(indexes);
		} catch (MongoCommandException e) {
			if (e.getErrorCode() == 26) {
				// NamespaceNotFound
				// ignore
				return;
			}
			throw e;
		}
	}

	private void ensureSearchIndex(Class<? extends RelayModel> clazz, String collectionName, List<Document> existing) {
		List<AtlasIndex> atlasIndexes = List.of(clazz.getAnnotationsByType(AtlasIndex.class));
		List<SearchIndexModel> indexes = atlasIndexes.stream().map((atlasIndex) -> {
					Document definition =
							new Document(
									"mappings",
									new Document("dynamic", atlasIndex.dynamic())
											.append("fields", buildSearchFields(clazz, atlasIndex.custom()))
							);

					if (!atlasIndex.analyzer().isEmpty()) {
						definition.append("analyzer", atlasIndex.analyzer());
					}

					if (!atlasIndex.searchAnalyzer().isEmpty()) {
						definition.append("searchAnalyzer", atlasIndex.searchAnalyzer());
					}

					// numPartitions
					if (atlasIndex.numPartitions() > 0) {
						definition.append("numPartitions", atlasIndex.numPartitions());
					}

					// custom analyzers
					if (atlasIndex.analyzers().length > 0) {
						definition.append("analyzers", buildCustomAnalyzers(atlasIndex.analyzers()));
					}

					// storedSource
					StoredSource storedSource = atlasIndex.storedSource();
					if (storedSource.enabled()) {
						if (storedSource.include().length == 0 && storedSource.exclude().length == 0) {
							definition.append("storedSource", true);
						} else {
							Document storedSourceFields = new Document();
							if (storedSource.include().length  > 0) {
								storedSourceFields.append("include", List.of(storedSource.include()));
							}
							if (storedSource.exclude().length  > 0) {
								storedSourceFields.append("exclude", List.of(storedSource.exclude()));
							}
							definition.append("storedSource", storedSourceFields);
						}
					}

					// synonyms
					if (atlasIndex.synonyms().length > 0) {
						List<Document> synonyms = new ArrayList<>();
						for (Synonym synonym : atlasIndex.synonyms()) {
							synonyms.add(new Document("name", synonym.name())
									.append("source", new Document("collection", synonym.sourceCollection()))
									.append("analyzer", synonym.analyzer()));
						}
						definition.append("synonyms", synonyms);
					}

					return new SearchIndexModel(
							atlasIndex.name(),
							definition,
							SearchIndexType.search()
					);
				})
				.filter((next) -> existing.stream().noneMatch((which) -> which.get("name", "").equals(next.getName())))
				.collect(Collectors.toList());
		if (indexes.isEmpty()) {
			return;
		}
		RelayCollection<Document> collection = this.on(collectionName).getCollection();
		try {
			collection.createSearchIndexes(indexes);
		} catch (MongoCommandException e) {
			if (e.getErrorCode() == 26) {
				// NamespaceNotFound
				// ignore
				return;
			}
			throw e;
		}
	}

	private static List<Document> buildCustomAnalyzers(AnalyzerDef[] analyzers) {
		List<Document> docs = new ArrayList<>();
		for (AnalyzerDef analyzer : analyzers) {
			Document analyzerDefinition = new Document("name", analyzer.name());

			// tokenizer
			Document tokenizer = new Document("type", analyzer.tokenizer());
			if (analyzer.minGram() > 0) {
				tokenizer.append("minGram", analyzer.minGram());
			}
			if (analyzer.maxGram() > 0) {
				tokenizer.append("maxGram", analyzer.maxGram());
			}
			if (analyzer.maxTokenLength() > 0) {
				tokenizer.append("maxTokenLength", analyzer.maxTokenLength());
			}
			if (!analyzer.pattern().isEmpty()) {
				tokenizer.append("pattern", analyzer.pattern());
			}
			if (analyzer.group() > 0) {
				tokenizer.append("group", analyzer.group());
			}
			analyzerDefinition.append("tokenizer", tokenizer);

			// charFilters
			if (analyzer.charFilters().length > 0) {
				List<Document> charFilters = new ArrayList<>();
				for (CharFilterDef charFilter : analyzer.charFilters()) {
					Document charFilterDef = new Document("type", charFilter.type());
					if (charFilter.ignoredTags().length > 0) {
						charFilterDef.append("ignoredTags", List.of(charFilter.ignoredTags()));
					}
					if (charFilter.mapping().length > 0) {
						Document map = new Document();
						for (Mapping mapping : charFilter.mapping()) {
							map.append(mapping.key(), mapping.value());
						}
						charFilterDef.append("mapping", map);
					}
					charFilters.add(charFilterDef);
				}
				analyzerDefinition.append("charFilters", charFilters);
			}

			// tokenFilters
			if (analyzer.tokenFilters().length > 0) {
				List<Document> tokenFilters = new ArrayList<>();
				for (TokenFilterDef tokenFilter : analyzer.tokenFilters()) {
					Document tokenFilterDef = new Document("type", tokenFilter.type());

					// nGram
					if (tokenFilter.minGram() > 0) {
						tokenFilterDef.append("minGram", tokenFilter.minGram());
					}
					if (tokenFilter.maxGram() > 0) {
						tokenFilterDef.append("maxGram", tokenFilter.maxGram());
					}
					if (!tokenFilter.termNotInBounds().isEmpty()) {
						tokenFilterDef.append("termNotInBounds", tokenFilter.termNotInBounds());
					}

					// stopword
					if (tokenFilter.tokens().length > 0) {
						tokenFilterDef.append("tokens", List.of(tokenFilter.tokens()));
					}
					if (!tokenFilter.ignoreCase()) {
						tokenFilterDef.append("ignoreCase", false);
					}

					// length
					if (tokenFilter.min() > 0) {
						tokenFilterDef.append("min", tokenFilter.min());
					}
					if (tokenFilter.max() > 0) {
						tokenFilterDef.append("max", tokenFilter.max());
					}

					// regex
					if (!tokenFilter.pattern().isEmpty()) {
						tokenFilterDef.append("pattern", tokenFilter.pattern())
								.append("replacement", tokenFilter.replacement());
					}
					if (!tokenFilter.matches().isEmpty()) {
						tokenFilterDef.append("matches", tokenFilter.matches());
					}

					// shingle
					if (tokenFilter.minShingleSize() > 0) {
						tokenFilterDef.append("minShingleSize", tokenFilter.minShingleSize());
					}
					if (tokenFilter.maxShingleSize() > 0) {
						tokenFilterDef.append("maxShingleSize", tokenFilter.maxShingleSize());
					}

					// wordDelimiterGraph
					DelimiterOptions delimiterOptions = tokenFilter.delimiterOptions();
					if (Objects.equals(tokenFilter.type(), "wordDelimiterGraph") && Objects.nonNull(delimiterOptions)) {
						Document opts = new Document()
								.append("generateWordParts", delimiterOptions.generateWordParts())
								.append("generateNumberParts", delimiterOptions.generateNumberParts())
								.append("concatenateWords", delimiterOptions.concatenateWords())
								.append("concatenateNumbers", delimiterOptions.concatenateNumbers())
								.append("concatenateAll", delimiterOptions.concatenateAll())
								.append("preserveOriginal", delimiterOptions.preserveOriginal())
								.append("splitOnCaseChange", delimiterOptions.splitOnCaseChange())
								.append("splitOnNumerics", delimiterOptions.splitOnNumerics())
								.append("stemEnglishPossessive", delimiterOptions.stemEnglishPossessive())
								.append("ignoreKeywords", delimiterOptions.ignoreKeywords());
						tokenFilterDef.append("delimiterOptions", opts);
					}
					// protectedWords
					ProtectedWords protectedWords = tokenFilter.protectedWords();
					if (Objects.nonNull(protectedWords) && protectedWords.words().length > 0) {
						tokenFilterDef.append("protectedWords", new Document("words", List.of(protectedWords.words()))
								.append("ignoreCase", protectedWords.ignoreCase()));
					}

					// phonetic & folding
					if (!tokenFilter.originalTokens().isEmpty()) {
						tokenFilterDef.append("originalTokens", tokenFilter.originalTokens());
					}

					// icuNormalizer
					if (!tokenFilter.normalizationForm().isEmpty()) {
						tokenFilterDef.append("normalizationForm", tokenFilter.normalizationForm());
					}

					// snowballStemming
					if (!tokenFilter.stemmerName().isEmpty()) {
						tokenFilterDef.append("stemmerName", tokenFilter.stemmerName());
					}

					tokenFilters.add(tokenFilterDef);
				}
				analyzerDefinition.append("tokenFilters", tokenFilters);
			}
			docs.add(analyzerDefinition);
		}
		return docs;
	}

	private static Document buildSearchFields(Class<?> clazz) {
		Document fieldsDoc = new Document();

		for (Field field : clazz.getDeclaredFields()) {
			AtlasField[] annotations = field.getAnnotationsByType(AtlasField.class);
			if (annotations.length == 0) continue;

			List<Document> definitions = Stream.of(annotations)
					.map(annotation -> MongoRelay.buildFieldDefinition(annotation, field))
					.collect(Collectors.toList());

			fieldsDoc.append(field.getName(), definitions.size() == 1 ? definitions.get(0) : definitions);
		}

		return fieldsDoc;
	}

	private static Document buildFieldDefinition(AtlasField annotation, Field field) {
		Document definition = new Document("type", annotation.type().getType());
		switch (annotation.type()) {
			case STRING:
				if (!annotation.analyzer().isEmpty()) {
					definition.append("analyzer", annotation.analyzer());
				}
				if (!annotation.searchAnalyzer().isEmpty()) {
					definition.append("searchAnalyzer", annotation.searchAnalyzer());
				}
				if (!annotation.indexOptions().isEmpty()) {
					definition.append("indexOptions", annotation.indexOptions());
				}
				if (!annotation.store()) {
					definition.append("store", false);
				}
				if (annotation.ignoreAbove() > 0) {
					definition.append("ignoreAbove", annotation.ignoreAbove());
				}
				if (!annotation.norms().isEmpty()) {
					definition.append("norms", annotation.norms());
				}
				if (annotation.multi().length > 0) {
					Document multiDocument = new Document();
					for (MultiField multiField : annotation.multi()) {
						Document subField = MongoRelay.buildMultiProp(multiField);
						multiDocument.append(multiField.name(), subField);
					}
					definition.append("multi", multiDocument);
				}
				break;
			case AUTOCOMPLETE:
				if (!annotation.analyzer().isEmpty()) {
					definition.append("analyzer", annotation.analyzer());
				}
				if (!annotation.tokenization().isEmpty()) {
					definition.append("tokenization", annotation.tokenization());
				}
				if (annotation.minGrams() > 0) {
					definition.append("minGrams", annotation.minGrams());
				}
				if (annotation.maxGrams() > 0) {
					definition.append("maxGrams", annotation.maxGrams());
				}
				if (!annotation.foldDiacritics()) {
					definition.append("foldDiacritics", false);
				}
				break;
			case TOKEN:
				if (!annotation.normalizer().isEmpty()) {
					definition.append("normalizer", annotation.normalizer());
				}
				break;
			case NUMBER:
			case NUMBER_FACET:
				if (!annotation.representation().isEmpty()) {
					definition.append("representation", annotation.representation());
				}
				if (!annotation.indexIntegers()) {
					definition.append("indexIntegers", false);
				}
				if (!annotation.indexDoubles()) {
					definition.append("indexDoubles", false);
				}
				break;
			case KNN_VECTOR:
				definition.append("dimensions", annotation.dimensions())
						.append("similarity", annotation.similarity());
				break;
			case GEO:
				if (annotation.indexShapes()) {
					definition.append("indexShapes", true);
				}
				break;
			case DOCUMENT:
			case EMBEDDED_DOCUMENTS:
				definition.append("dynamic", annotation.dynamic());
				if (!annotation.dynamic()) {
					Class<?> nested = field.getType();
					if (Objects.equals(annotation.type(), EMBEDDED_DOCUMENTS) && List.class.isAssignableFrom(nested)) {
						nested = ClassUtils.resolveListElementType(field);
					}
					definition.append("fields", buildSearchFields(nested));
				}
				break;
			case STRING_FACET:
			case DATE:
			case DATE_FACET:
			case BOOLEAN:
			case OBJECT_ID:
			case UUID:
				//no extra props
				break;
			default:
				throw new IllegalArgumentException("Unknown AtlasField type: " + annotation.type());
		}
		return definition;
	}

	private static Document buildMultiProp(MultiField multiField) {
		Document subField = new Document("type", multiField.type().getType());
		if (!multiField.analyzer().isEmpty()) {
			subField.append("analyzer", multiField.analyzer());
		}
		if (!multiField.searchAnalyzer().isEmpty()) {
			subField.append("searchAnalyzer", multiField.searchAnalyzer());
		}
		if (!multiField.tokenization().isEmpty()) {
			subField.append("tokenization", multiField.tokenization());
		}
		if (multiField.minGrams() > 0) {
			subField.append("minGrams", multiField.minGrams());
		}
		if (multiField.maxGrams() > 0) {
			subField.append("maxGrams", multiField.maxGrams());
		}
		if (!multiField.foldDiacritics()) {
			subField.append("foldDiacritics", false);
		}
		return subField;
	}
}
