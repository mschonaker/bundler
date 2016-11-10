package io.github.mschonaker.bundler.utils;

@FunctionalInterface
public interface Coercer<S, T> {

	T coerce(S o, Class<T> target);

}
