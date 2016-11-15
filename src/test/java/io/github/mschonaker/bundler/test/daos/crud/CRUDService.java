package io.github.mschonaker.bundler.test.daos.crud;

public interface CRUDService {

	void createTables();

	User find(String username);

	void upsert(User user);

	void delete(String string);

	Long count();
}
