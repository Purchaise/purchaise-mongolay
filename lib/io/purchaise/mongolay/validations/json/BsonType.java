package io.purchaise.mongolay.validations.json;

import org.bson.BsonString;

public class BsonType {
    public static final BsonString OBJECT = new BsonString("object");
    public static final BsonString ARRAY = new BsonString("array");
    public static final BsonString STRING = new BsonString("string");
    public static final BsonString LONG = new BsonString("long");
    public static final BsonString INTEGER = new BsonString("int");
    public static final BsonString DECIMAL = new BsonString("decimal");
    public static final BsonString DOUBLE = new BsonString("double");
    public static final BsonString BOOLEAN = new BsonString("bool");
    public static final BsonString OBJECT_ID = new BsonString("objectId");
    public static final BsonString NULL = new BsonString("null");
}
