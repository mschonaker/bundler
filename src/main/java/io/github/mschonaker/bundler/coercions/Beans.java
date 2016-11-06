package io.github.mschonaker.bundler.coercions;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

public final class Beans {

	private Beans() {
	}

	public static void setNestedProperty(Object bean, String name, Object value, Coercions coercions) {

		try {
			int i = name.indexOf('.');

			if (i < 0) {

				PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
				if (descriptor != null)
					descriptor.getWriteMethod().invoke(bean, coercions.coerce(value, descriptor.getPropertyType()));
				return;
			}

			// Recursion.
			String prop = name.substring(0, i);

			PropertyDescriptor descriptor = getPropertyDescriptor(bean, prop);

			Object currentValue = descriptor.getReadMethod().invoke(bean);

			if (currentValue == null) {

				Class<?> propertyType = descriptor.getPropertyType();
				currentValue = propertyType.newInstance();
				descriptor.getWriteMethod().invoke(bean, coercions.coerce(currentValue, descriptor.getPropertyType()));

			}

			setNestedProperty(currentValue, name.substring(i + 1), value, coercions);

		} catch (Exception e) {

			String targetClassName = (bean == null ? null : bean.getClass().getName());
			String valueClassName = (value == null ? null : value.getClass().getName());
			String propertyName = targetClassName == null ? name : targetClassName + "." + name;

			throw new IllegalArgumentException("Unable to set property " + propertyName + " value of Class " + valueClassName, e);
		}

	}

	private static PropertyDescriptor getPropertyDescriptor(Object bean, String name) throws IntrospectionException {
		return Arrays.stream(Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors())//
				.filter(d -> d.getName().equals(name))//
				.findFirst()//
				// .orElseThrow(() -> new IntrospectionException("Unknown
				// property " + name + " for class " + bean.getClass()));
				.orElse(null);
	}
}
