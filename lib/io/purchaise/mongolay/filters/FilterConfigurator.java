package io.purchaise.mongolay.filters;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import io.purchaise.mongolay.utils.TextUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by agonlohaj on 14 Sep, 2022
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilterConfigurator {
	private List<CollectionRelation> relations = new ArrayList<>();
	private Class<?> target;
	private List<FiltersGroup> filterGroups = new ArrayList<>();
	// An ordered(!) set of hierarchy mappings that contain column/value pairs for multiple hierarchy groups
	private List<Map<String, Integer>> hierarchy = new ArrayList<>();
	private Set<Class<?>> priorities = new LinkedHashSet<>();
	private Map<String, Class<?>> classMap = new HashMap<>();

	public FilterConfigurator withClassPriority (Class<?> priority) {
		this.priorities.add(priority);
		return this;
	}

	public FilterConfigurator withFilterGroup (FiltersGroup relation) {
		this.filterGroups.add(relation);
		return this;
	}

	public void setFilterGroups(List<FiltersGroup> filterGroups) {
		this.filterGroups = filterGroups;
	}

	public Set<Class<?>> getTargets () {
		Set<Class<?>> targets = new LinkedHashSet<>();
		// otherwise construct
		targets.add(getTarget());
		Set<Class<?>> others = getFilterGroups().stream().reduce(new HashSet<>(), (acc, next) -> {
			acc.addAll(next.getTargets(classMap));
			return acc;
		}, (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toCollection(LinkedHashSet::new)));
		targets.addAll(others);
		return targets;
	}

	public Set<Class<?>> getPriorities () {
		Set<Class<?>> all = new LinkedHashSet<>();
		all.add(getTarget());
		all.addAll(priorities);
		Set<Class<?>> targets = getTargets();
		all.addAll(targets);

		// get all set of priorities that exists as a target at group filters
		return all.stream()
				.filter(targets::contains)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Gets the hierarchical groups for a general filter
	 * @param target target field
	 */
	public List<FiltersGroup> getHierarchicalGroups (String target) {
		if (TextUtils.isNullOrEmpty(target)) {
			return this.getFilterGroups();
		}
		if (this.getHierarchy().isEmpty()) {
			return this.getFilterGroups();
		}
		Map<String, Integer> hierarchy = Utils.getHierarchyGroup(this.getHierarchy(), target);
		return this.getFilterGroups().stream().peek(group -> {
			List<GeneralFilter<?>> filters = group.getFilters();
			List<GeneralFilter<?>> result = filters.stream().filter(filter -> {
				// if no hierarchy definition exists, assume your at the bottom of the pipe
				int current = hierarchy.getOrDefault(target, Integer.MAX_VALUE);
				// check against the next filter, if it doesn't exist in the hierarchy assume it always needs to be executed
				int next = hierarchy.getOrDefault(filter.getColumn(), -1);

				return next < current;
			}).collect(Collectors.toList());
			group.setFilters(result);
		}).collect(Collectors.toList());
	}


	/**
	 * Gets related groups for given configurator
	 * @param target target field
	 */
	public List<RelatedGroup> getRelatedGroups(String target) {
		// fill in the dependencies between classes and return the result
		List<FiltersGroup> groups = new ArrayList<>(this.getHierarchicalGroups(target));
		List<RelatedGroup> relatedGroups = new ArrayList<>();
		// fill in the dependencies between classes and return the result
		for (int i = 0; i < groups.size(); i++) {
			FiltersGroup current = groups.get(i);
			List<FiltersGroup> currentDependentGroups = Utils.getDependentGroups(current);
			RelatedGroup relatedGroup = new RelatedGroup(current.getOperator(), currentDependentGroups);
			if (current.isOr()) {
				relatedGroups.add(relatedGroup);
				while (current.isOr()) {
					if (i == groups.size() -1) {
						break;
					}
					FiltersGroup next = groups.get(i + 1);
					relatedGroup.getGroups().addAll(Utils.getDependentGroups(next));

					// remove the next group and merge it here
					groups.remove(i + 1);
					current = next;
				}
			} else {
				// split the groups
				for (FiltersGroup group: currentDependentGroups) {
					relatedGroups.add(new RelatedGroup(group));
				}
			}
		}
		return relatedGroups;
	}

	/**
	 * Builds executions steps for given configurator
	 */
	public List<Bson> execute() {
		// start performing filtering based on priority
		return this.execute(null);
	}

	/**
	 * Builds executions steps for given configurator with a target field specified
	 * @param column target column
	 */
	public List<Bson> execute(String column) {
		// start performing filtering based on priority
		List<RelatedGroup> groups = this.getRelatedGroups(column);
		List<RelatedGroup> clone = new ArrayList<>(groups);

		List<Bson> steps = new ArrayList<>();

		Set<Class<?>> priorities = this.getPriorities();
		Set<Class<?>> linkedCollections = new HashSet<>();
		linkedCollections.add(this.getTarget());

		Set<CollectionRelation> executedRelations = new HashSet<>();
		//groups.forEach(next -> System.out.println("Next group: " + next.toString()));

		// if the target of a filter is not the same as the target collection perform lookups in between
		// if there are filters or filters groups that contain an or operation between different collection, then evaluate the dependency requirements
		// before building a plan for a priority class, see if lookups are needed
		for (Class<?> clazz: priorities) {
			// check if this relationship is needed in the first place
			boolean required = groups.stream().anyMatch(group -> group.isRequired(clazz, this.getClassMap()));
			// we expect that at priorities the first class is the target
			if (!this.getTarget().equals(clazz) && required) {
				// a new class needs to be looked up
				Optional<CollectionRelation> optional = this.getRelations()
						.stream()
						.filter(next -> next.getFrom().equals(this.getTarget()) && next.getTo().equals(clazz))
						.findFirst();
				if (optional.isPresent()) {
					CollectionRelation relation = optional.get();

					steps.addAll(relation.build());
					executedRelations.add(relation);
					linkedCollections.add(clazz);
				}
			}
			//steps.addAll(onRelationshipAdded.apply(clazz));
			// see through all the groups if you can apply a step
			// then create one lookup and repeat until all groups are covered
			for (int i = groups.size() - 1; i >= 0; i--) {
				RelatedGroup group = groups.get(i);
				// check if dependencies are met
				if (group.isExecutable(linkedCollections, this.getClassMap())) {
					// I can perform this group
					Bson filters = group.execute(executedRelations, this.getClassMap());
					steps.add(filters);
					groups.remove(i);
				}
			}
		}
		// check if there are any in array filters
		clone.stream()
				.filter((next) -> next.isExecutable(linkedCollections, this.getClassMap()))
				.flatMap(next -> next.getGroups().stream())
				.filter(FiltersGroup::isExecutable)
				.flatMap(next -> next.getFilters().stream().peek(filter -> filter.setExclude(filter.isExclude() ^ next.isExclude())))
				.filter(GeneralFilter::isInArrayFilter)
				.forEach((generalFilter) -> {
					GeneralFilter<?> next = generalFilter.clone();
					String which = next.getColumn(executedRelations, this.getClassMap());

					Deque<String> stack = new ArrayDeque<>(Arrays.asList(which.split("\\.")));
					String field = stack.pollLast();
					String rest = String.join(".", stack);

					next.setColumn("$$item." + field);
					Bson filter = next.buildFilter(new HashSet<>(), this.getClassMap());

					steps.add(Aggregates.addFields(new Field<>(rest, new Document("$filter", new Document("input", "$" + rest)).append("as", "item").append("cond", filter))));
				});

		return steps;
	}
}
