package io.github.mschonaker.bundler.test.daos.updates;

import java.util.List;

public interface UserDAO {

	void createSchema();

	List<User> findAll(int offset, int limit);

	User find(String string);

	Long count();

	void insert(User user);

	void update(User user);

	void upsert(User user);

	void delete(String username);
}
