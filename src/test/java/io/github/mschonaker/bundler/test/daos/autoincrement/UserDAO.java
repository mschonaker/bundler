package io.github.mschonaker.bundler.test.daos.autoincrement;

import java.util.List;

public interface UserDAO {

	void createSchema();

	List<User> findAll();

	User find(Long id);

	Long insert(User user);

}
