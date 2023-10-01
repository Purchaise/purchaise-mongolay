package io.purchaise.mongolay;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Collections;

import static org.bson.codecs.pojo.Conventions.ANNOTATION_CONVENTION;

/**
 * Created by Agon Lohaj on 29 Oct, 2020
 */
public class Main {

	public static void main(String[] args) {
		// Nothing to do here
		CodecProvider pojoCodecProvider =
				PojoCodecProvider.builder()
						.conventions(Collections.singletonList(ANNOTATION_CONVENTION))
						.register("io.purchaise.mongolay")
						.automatic(true).build();

		final CodecRegistry customEnumCodecs = CodecRegistries.fromCodecs();
		CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), customEnumCodecs, CodecRegistries.fromProviders(pojoCodecProvider));

		MongoClient client = MongoClients.create("mongodb://localhost:27017");
		MongoDatabase dev = client.getDatabase("purchaise").withCodecRegistry(pojoCodecRegistry);
//		MongoRelayClient mongoRelayClient = new MongoRelayClient(dev, master);
		MongoRelay mongoRelay = new MongoRelay(dev).withMaxDepth(1);
	}
}
