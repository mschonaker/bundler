package io.github.mschonaker.bundler;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.mschonaker.bundler.coercions.Beans;
import io.github.mschonaker.bundler.coercions.Coercions;

/**
 * A class around a {@link ResultSet} with special target-type capabilities.
 *
 * @author mschonaker
 */
class Result {

	/**
	 * An interface for iteration.
	 */
	static interface OnEach {

		<T> T onEach(Class<T> type, T object) throws Exception;

	}

	private final ResultSet rs;
	private final String[] targetPropertyNames;
	private final Coercions coercions;

	Result(ResultSet rs, Coercions coercions) throws SQLException {
		this.rs = rs;
		targetPropertyNames = obtainTargetPropertyNames(rs);
		this.coercions = coercions;
	}

	private static String[] obtainTargetPropertyNames(ResultSet rs) throws SQLException {

		ResultSetMetaData rmd = rs.getMetaData();

		String[] targetPropertyNames = new String[rmd.getColumnCount()];

		for (int i = 0; i < rmd.getColumnCount(); i++) {

			String targetPropertyName = rmd.getColumnLabel(i + 1);
			if (targetPropertyName == null || targetPropertyName.isEmpty())
				targetPropertyName = rmd.getColumnLabel(i + 1);

			targetPropertyNames[i] = toCamelCase(targetPropertyName);
		}

		return targetPropertyNames;
	}

	private static String toCamelCase(String value) {

		String[] split = value.split("_");

		return split[0].toLowerCase() + //
				Arrays.stream(split, 1, split.length)//
						.map(String::toLowerCase)//
						.map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))//
						.collect(Collectors.joining());
	}

	public <T> List<T> asListOf(Class<T> targetClass, OnEach onEach) throws Exception {

		List<T> list = new LinkedList<T>();
		while (rs.next()) {

			T object = targetClass.newInstance();

			for (int i = 0; i < targetPropertyNames.length; i++) {
				Object value = rs.getObject(i + 1);

				String property = targetPropertyNames[i];
				Beans.setNestedProperty(object, property, value, coercions);
			}

			list.add(onEach.onEach(targetClass, object));
		}

		return list;
	}

	public <T> List<T> asScalarListOf(Class<T> targetClass) throws Exception {

		if (targetPropertyNames.length != 1)
			throw new IllegalArgumentException("Couldn't unbox. Have more than one column: " + targetPropertyNames.length);

		List<T> list = new LinkedList<T>();

		while (rs.next())
			list.add(targetClass.cast(coercions.coerce(rs.getObject(1), targetClass)));

		return list;
	}

	public <T> T asOneOf(Class<T> targetClass, OnEach onEach) throws Exception {

		if (!rs.next())
			return null;

		T object = targetClass.newInstance();

		for (int i = 0; i < targetPropertyNames.length; i++) {
			Object value = rs.getObject(i + 1);

			String propertyName = targetPropertyNames[i];
			Beans.setNestedProperty(object, propertyName, value, coercions);
		}

		if (rs.next())
			throw new IllegalStateException("Query has more than one result");

		return onEach.onEach(targetClass, object);
	}

	public <T> T asScalarOf(Class<T> targetClass) throws Exception {

		if (targetPropertyNames.length != 1)
			throw new IllegalArgumentException("Couldn't unbox. Have more than one column: " + targetPropertyNames.length);

		if (!rs.next())
			return null;

		Object value = rs.getObject(1);

		if (rs.next())
			throw new IllegalStateException("Query has more than one result");

		return targetClass.cast(coercions.coerce(value, targetClass));
	}
}