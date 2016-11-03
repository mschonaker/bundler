package io.github.mschonaker.bundler.support;

public interface Converter {

	public Object convert(Class<?> type, Object value);

}