package io.purchaise.mongolay.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FieldType {
	STRING("string"),
	AUTOCOMPLETE("autocomplete"),
	TOKEN("token"),
	NUMBER("number"),
	NUMBER_FACET("numberFacet"),
	KNN_VECTOR("knnVector"),
	GEO("geo"),
	DOCUMENT("document"),
	EMBEDDED_DOCUMENTS("embeddedDocuments"),
	STRING_FACET("stringFacet"),
	DATE("date"),
	DATE_FACET("dateFacet"),
	BOOLEAN("boolean"),
	OBJECT_ID("objectId"),
	UUID("uuid");

	private final String type;
}
