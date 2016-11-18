package io.github.mschonaker.bundler.test.daos.helloworld;

import java.util.List;

public interface UserDAO {
	
	void createDatabase();
	
	List<User> findAll();

}
