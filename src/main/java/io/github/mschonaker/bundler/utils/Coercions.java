package io.github.mschonaker.bundler.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Coercions {

	public static final Coercions JRE;

	static {

		JRE = new Coercions();
		JRE.add(Object.class, String.class, (o, t) -> o.toString());
		JRE.add(String.class, Integer.class, (o, t) -> Integer.parseInt(o));
		JRE.add(Number.class, Boolean.class, (o, t) -> o.intValue() != 0);
		JRE.add(String.class, Enum.class, (o, t) -> Enum.valueOf(t, o));
		JRE.add(Integer.class, Enum.class, (o, t) -> t.getEnumConstants()[o]);
		JRE.add(Enum.class, Integer.class, (o, t) -> o.ordinal());
		JRE.add(byte[].class, InputStream.class, (o, t) -> new ByteArrayInputStream(o));

		JRE.add(Blob.class, InputStream.class, (o, t) -> {
			try {

				Blob blob = Blob.class.cast(o);
				return blob.getBinaryStream();

			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});

	}

	private static class Key {

		final Class<?> source, target;

		public Key(Class<?> source, Class<?> target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Coerce: " + source + " -> " + target;
		}
	}

	private final Map<Key, Coercer<?, ?>> coercers;

	public Coercions() {
		coercers = new HashMap<>();
	}

	public Coercions(Coercions other) {
		coercers = new HashMap<>(other.coercers);
	}

	public Coercions addAll(Coercions other) {
		coercers.putAll(other.coercers);
		return this;
	}

	public <S, T> Coercions add(Class<S> source, Class<T> target, Coercer<S, T> coercer) {
		coercers.put(new Key(source, target), coercer);
		return this;
	}

	public <S, T> T coerce(S o, Class<T> target) {

		// null has no type.
		if (o == null)
			return null;

		// identity.
		if (target.isAssignableFrom(o.getClass()))
			return target.cast(o);

		Coercer<S, T> coercer = find((Class<S>) o.getClass(), target);
		if (coercer != null)
			return coercer.coerce(o, target);

		throw new ClassCastException("Couldn't coerce from " + o.getClass() + " to " + target);
	}

	public <S, T> Coercer<S, T> find(Class<S> source, Class<T> target) {

		Class<S> t = source;

		while (t != null) {

			Key k = new Key(t, target.isEnum() ? Enum.class : target);

			Coercer<S, T> c = (Coercer<S, T>) coercers.get(k);

			if (c != null)
				return c;

			t = (Class<S>) t.getSuperclass();
		}

		for (Class<?> i : source.getInterfaces()) {
			Coercer<?, T> c = find(i, target);
			if (c != null)
				return (Coercer<S, T>) c;
		}

		return null;
	}
}
