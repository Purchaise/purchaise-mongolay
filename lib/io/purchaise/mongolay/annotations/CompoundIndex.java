package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(CompoundIndexes.class)
public @interface CompoundIndex {
    Index[] indexes();
}
