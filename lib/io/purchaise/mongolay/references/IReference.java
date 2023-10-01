package io.purchaise.mongolay.references;


import io.purchaise.mongolay.MongoRelay;

import java.util.Collection;

public interface IReference {
    <A extends Collection<? super TResult>, TResult> A map(A item, MongoRelay mongoRelay);
    boolean isValid();
}
