package io.purchaise.mongolay.filters;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public @Data class FiltersException extends RuntimeException{
	private int statusCode;
	private String message;

	public FiltersException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.message = message;
	}
}
