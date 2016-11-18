package io.github.mschonaker.bundler.test.daos.autoincrement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Config;
import io.github.mschonaker.bundler.Transaction;
import io.github.mschonaker.bundler.test.AbstractTest;

public class AutoincrementTest extends AbstractTest {

	private static UserDAO dao;

	@BeforeClass
	public static void beforeClass() throws IOException {
		dao = initialize(UserDAO.class, new Config().strict());
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.createSchema();
			tx.success();
		}
	}

	@Test
	public void testInsert() {

		Long id;
		try (Transaction tx = Bundler.writeTransaction(ds)) {

			User user = new User();
			user.setUsername("user-a");
			user.setPassword("secret");
			user.setRealname("User A");

			id = dao.insert(user);
			assertNotNull(id);

			user = new User();
			user.setUsername("user-b");
			user.setPassword("secret");
			user.setRealname("User B");
			Long id2 = dao.insert(user);
			assertNotNull(id2);

			assertNotEquals(id, id2);

			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			User user = dao.find(id);
			assertEquals("user-a", user.getUsername());
			assertEquals("secret", user.getPassword());
			assertEquals("User A", user.getRealname());
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			List<User> users = dao.findAll();
			Set<String> set = users.stream().map(u -> u.getUsername()).collect(Collectors.toSet());
			assertTrue(set.contains("user-a"));
			assertTrue(set.contains("user-b"));
		}
	}
}
