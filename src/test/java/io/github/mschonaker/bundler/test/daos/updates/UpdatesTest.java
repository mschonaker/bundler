package io.github.mschonaker.bundler.test.daos.updates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Config;
import io.github.mschonaker.bundler.Transaction;
import io.github.mschonaker.bundler.test.AbstractTest;

public class UpdatesTest extends AbstractTest {

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
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user = new User();
			user.setUsername("user-a");
			user.setPassword("secret");
			user.setRealname("User A");
			dao.insert(user);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<User> users = dao.findAll(0, 10);
			assertEquals(1, users.size());

			assertEquals(1, dao.count(), 0);

			User user = dao.find("user-a");
			assertNotNull(user);
			assertEquals("secret", user.getPassword());
			assertEquals("User A", user.getRealname());

		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.delete("user-a");
			assertEquals(0, dao.count(), 0);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, dao.count(), 0);
		}
	}

	@Test
	public void testUpdate() {

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, dao.count(), 0);
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user = new User();
			user.setUsername("user-a");
			user.setPassword("secret");
			user.setRealname("User A");
			dao.insert(user);
			tx.success();
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user = dao.find("user-a");
			user.setRealname("User NEW A");
			dao.update(user);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<User> users = dao.findAll(0, 10);
			assertEquals(1, users.size());

			assertEquals(1, dao.count(), 0);

			User user = dao.find("user-a");
			assertNotNull(user);
			assertEquals("secret", user.getPassword());
			assertEquals("User NEW A", user.getRealname());
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.delete("user-a");
			assertEquals(0, dao.count(), 0);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, dao.count(), 0);
		}
	}

	@Test
	public void testUpsert() {

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, dao.count(), 0);
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user = new User();
			user.setUsername("user-a");
			user.setPassword("secret");
			user.setRealname("User A");
			dao.insert(user);
			tx.success();
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user = dao.find("user-a");
			user.setRealname("User NEW A");
			dao.upsert(user);

			user = new User();
			user.setUsername("user-b");
			user.setPassword("secret");
			user.setRealname("User B");
			dao.upsert(user);

			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<User> users = dao.findAll(0, 10);
			assertEquals(2, users.size());

			assertEquals(2, dao.count(), 0);

			User user = dao.find("user-a");
			assertNotNull(user);
			assertEquals("secret", user.getPassword());
			assertEquals("User NEW A", user.getRealname());

			user = dao.find("user-b");
			assertNotNull(user);
			assertEquals("secret", user.getPassword());
			assertEquals("User B", user.getRealname());
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.delete("user-a");
			assertEquals(1, dao.count(), 0);
			dao.delete("user-b");
			assertEquals(0, dao.count(), 0);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, dao.count(), 0);
		}
	}
}
