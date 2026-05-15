package io.purchaise.mongolay.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(CompoundIndexes.class)
public @interface CompoundIndex {
    Index[] indexes();

    /**
     * When {@code true}, the index is created with the unique constraint
     * ({@code IndexOptions.unique = true}). Defaults to {@code false} for
     * backward compatibility — existing annotations behave unchanged.
     *
     * <p>Required for {@code $merge} target collections: MongoDB error 51183
     * ("Cannot find index to verify that join fields will be unique") is raised
     * when the {@code on} fields of a merge stage have no unique index. If an
     * index with the same key signature already exists with the opposite
     * uniqueness, mongolay drops it and recreates it to honor this flag.</p>
     */
    boolean unique() default false;
}
