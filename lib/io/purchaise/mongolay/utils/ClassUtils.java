package io.purchaise.mongolay.utils;

import io.purchaise.mongolay.annotations.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by agonlohaj on 23 Mar, 2021
 */
public class ClassUtils {

	public static <T> boolean isCollectionField (Class<T> type) {
		if (Collection.class.isAssignableFrom(type)) {
			return true;
		} else if (Map.class.isAssignableFrom(type)) {
			return true;
		} else if (Set.class.isAssignableFrom(type)) {
			return true;
		}
		return false;
	}

	public static Class<?> getTargetClass(Field which) {
		if (ClassUtils.isCollectionField(which.getType())) {
			ParameterizedType integerListType = (ParameterizedType) which.getGenericType();
			return (Class<?>) integerListType.getActualTypeArguments()[0];
		}
		return which.getType();
	}

	public static <T> String entityName (Class<T> clazz) {
		Entity[] next = clazz.getAnnotationsByType(Entity.class);
		if (next.length == 0) {
			return clazz.getSimpleName().toLowerCase();
		}

		Entity entity = next[0];
		return entity.collection();
	}
}
