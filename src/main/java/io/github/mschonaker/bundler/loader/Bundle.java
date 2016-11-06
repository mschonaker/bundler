package io.github.mschonaker.bundler.loader;

import java.util.List;
import java.util.Map;

public class Bundle {

	public String name;

	public String sql;

	public List<String> expressions;

	public Map<String, Bundle> children;

	@Override
	public String toString() {
		return "Bundle [name=" + name + ", sql=" + sql + ", expressions=" + expressions + ", children=" + children + "]";
	}

}
