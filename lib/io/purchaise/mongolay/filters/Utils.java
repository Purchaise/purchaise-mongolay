package io.purchaise.mongolay.filters;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utils {
	/**
	 * Get class object from configurator's class map depending on the filter
	 * @param classMap the class map
	 * @param filter the filter
	 * @return class object that corresponds with filter's collection
	 */
	public static Class<?> getClassFrom(Map<String, Class<?>> classMap, GeneralFilter<?> filter) {
		return classMap.getOrDefault(filter.getCollection(), null);
	}

	/**
	 * Find the hierarchy group for a given target field
	 * @param hierarchy the map
	 * @param target target field
	 * @return hierarchy value
	 */
	public static Map<String, Integer> getHierarchyGroup(List<Map<String, Integer>> hierarchy, String target) {
		return hierarchy.stream()
				.filter(next -> next.keySet().stream().anyMatch(target::equals))
				.findFirst()
				.orElse(new HashMap<>());
	}

	/**
	 * Generate the hierarchy that maps field to the hierarchical value
	 * @param fields list of fields
	 * @return map of fields and their corresponding hierarchy value
	 */
	public static Map<String, Integer> generateHierarchy (List<String> fields) {
		return IntStream.range(0, fields.size())
				.boxed()
				.collect(Collectors.toMap(fields::get, i -> i + 1));
	}

	public static List<FiltersGroup> getDependentGroups(FiltersGroup filtersGroup) {
		List<FiltersGroup> items = new ArrayList<>();
		if (filtersGroup.isOr()) {
			items.add(filtersGroup);
			return items;
		}

		// try to split in as much AND-s as possible
		List<GeneralFilter<?>> filters = new ArrayList<>(filtersGroup.getFilters());
		for (int i = 0; i < filters.size(); i++) {
			GeneralFilter<?> original = filters.get(i);
			FiltersGroup group = new FiltersGroup(UUID.randomUUID().toString(), filtersGroup.isExclude(), filtersGroup.getOperator(), new ArrayList<>(List.of(original)));
			items.add(group);
			GeneralFilter<?> current = original.clone();
			if (current.isOr()) {
				// if it is an or, then put together all or statements
				while (i != filters.size() - 1 && current.isOr()) {
					GeneralFilter<?> next = filters.get(i + 1);
					group.getFilters().add(next);

					// remove the next group and merge it here
					filters.remove(i + 1);
					current = next;
				}
			} else {
				// if it is an and, then put together all and statements
				while (i != filters.size() - 1 && !filters.get(i + 1).isOr() && filters.get(i + 1).getCollection().equals(original.getCollection())) {
					GeneralFilter<?> next = filters.get(i + 1);
					group.getFilters().add(next);

					// remove the next group and merge it here
					filters.remove(i + 1);
				}
			}
		}
		return items;
	}
}
