package io.github.mschonaker.bundler.test.daos.enums;

import java.util.List;

public interface EnumService {

	void createTables();

	void empty();

	void insert(SimpleEnum e);

	List<String> listStrings();

	List<Integer> listIntegers();

	List<SimpleEnum> listEnumsAsStrings();

	List<SimpleEnum> listEnumsAsIntegers();
}