package io.github.mschonaker.bundler.utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public final class Beans {

	private Beans() {
	}

	public static Object getNestedProperty(Object bean, String name, boolean lenient) {

		try {
			int i = name.indexOf('.');

			if (i < 0) {
				PropertyDescriptor descriptor = new PropertyDescriptor(name, bean.getClass());
				return descriptor.getReadMethod().invoke(bean);
			}

			// Recursion.
			String prop = name.substring(0, i);

			PropertyDescriptor descriptor = new PropertyDescriptor(prop, bean.getClass());

			Object currentValue = descriptor.getReadMethod().invoke(bean);

			return getNestedProperty(currentValue, name.substring(i + 1), lenient);

		} catch (Exception e) {

			if (lenient)
				return null;

			String targetClassName = (bean == null ? null : bean.getClass().getName());
			String propertyName = targetClassName == null ? name : targetClassName + "." + name;

			throw new IllegalArgumentException("Unable to get property " + propertyName, e);
		}

	}

	public static void setNestedProperty(Object bean, String name, Object value, boolean lenient, Coercions coercions) {

		try {
			int i = name.indexOf('.');

			if (i < 0) {

				PropertyDescriptor descriptor = new PropertyDescriptor(name, bean.getClass());
				if (descriptor != null)
					descriptor.getWriteMethod().invoke(bean, coercions.coerce(value, descriptor.getPropertyType()));
				return;
			}

			// Recursion.
			String prop = name.substring(0, i);

			PropertyDescriptor descriptor = new PropertyDescriptor(prop, bean.getClass());

			Object currentValue = descriptor.getReadMethod().invoke(bean);

			if (currentValue == null) {

				Class<?> propertyType = descriptor.getPropertyType();
				currentValue = propertyType.newInstance();
				descriptor.getWriteMethod().invoke(bean, coercions.coerce(currentValue, descriptor.getPropertyType()));

			}

			setNestedProperty(currentValue, name.substring(i + 1), value, lenient, coercions);

		} catch (Exception e) {

			if (lenient)
				return;

			String targetClassName = (bean == null ? null : bean.getClass().getName());
			String valueClassName = (value == null ? null : value.getClass().getName());
			String propertyName = targetClassName == null ? name : targetClassName + "." + name;

			throw new IllegalArgumentException(
					"Unable to set property " + propertyName + " value of Class " + valueClassName, e);
		}

	}

	public static Method getReadMethod(String name, Class<?> returnType) throws IntrospectionException {
		return new PropertyDescriptor(name, returnType).getReadMethod();
	}
}
