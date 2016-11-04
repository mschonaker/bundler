package io.github.mschonaker.bundler.coercions;

@FunctionalInterface
public interface Coercer<S, T> {

	T coerce(S o, Class<T> target);

}
