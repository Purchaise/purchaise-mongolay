package io.purchaise.mongolay.utils;

import io.purchaise.mongolay.RelayDatabase;
import io.purchaise.mongolay.options.OptionsUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xhemil on 09 Feb, 2025
 */
public class AggregationUtils {
	/**
	 * Processes a `$lookup` stage by merging calculation options if applicable and applying overridden collection names if any.
	 *
	 * @param database the database context used to retrieve options and entities.
	 * @param stage the `$lookup` stage document to be processed.
	 * @return a modified `$lookup` stage document with merged calculation options, or the original stage if no options apply.
	 */
	public static Bson handleLookup(RelayDatabase<?> database, Document stage) {
		Document lookupStage = stage.get("$lookup", Document.class);
		String from = lookupStage.getString("from");
		// handling of the dynamically named collections
		lookupStage.put("from", ClassUtils.getDynamicEntityName(database, lookupStage.getString("from")));

		if (!OptionsUtils.hasCalculationOptions(database, from)) {
			return stage;
		}

		Class<?> clazz = database
			.getMongoRelay()
			.getOptions()
			.keySet()
			.stream()
			.filter(next -> ClassUtils.isDynamicEntity(next) ? from.startsWith(ClassUtils.entityName(next)) : ClassUtils.entityName(next).equals(from))
			.findFirst()
			.orElse(null);

		List<? extends Bson> pipeline = lookupStage.getList("pipeline", Document.class, new ArrayList<>());
		List<? extends Bson> processedPipeline = OptionsUtils.mergeCalculationOptions(database, clazz, pipeline);
		lookupStage.put("pipeline", processedPipeline);
		return new Document("$lookup", lookupStage);
	}
}
