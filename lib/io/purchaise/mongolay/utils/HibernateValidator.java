package io.purchaise.mongolay.utils;

import io.purchaise.mongolay.Http;
import io.purchaise.mongolay.RelayException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class HibernateValidator {

	public static <T> Set<ConstraintViolation<T>> apply(T t) {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		return validator.validate(t);
	}

	public static <T> T validate(T t) throws RelayException {
		Set<ConstraintViolation<T>> errors = HibernateValidator.apply(t);
		if (errors.size() != 0) {
			throw new RelayException(Http.Status.BAD_REQUEST, HibernateValidator.formatErrors(errors), HibernateValidator.extractKeys(errors));
		}
		return t;
	}

	public static <T> boolean isValid(T t) {
		return HibernateValidator.apply(t).size() == 0;
	}

	public static <T> CompletableFuture<T> validateAsynch(T t, Executor context) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				HibernateValidator.validate(t);
			} catch (RelayException e) {
				e.printStackTrace();
				throw new CompletionException(e);
			}
			return t;
		}, context);
	}

	public static <T> List<Object> formatErrors (Set<ConstraintViolation<T>> errors) {
		return errors.stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.toList());
	}

	public static <T> List<Object> extractKeys (Set<ConstraintViolation<T>> errors) {
		return errors.stream()
				.map(err -> err.getPropertyPath().toString())
				.collect(Collectors.toList());
	}
}
