package io.github.mschonaker.bundler.test.daos.helloworld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Transaction;
import io.github.mschonaker.bundler.test.AbstractTest;

public class HelloworldTest extends AbstractTest {

	private static UserDAO dao;

	@BeforeClass
	public static void beforeClass() throws IOException {

		JdbcDataSource dataSource = new JdbcDataSource();

		dataSource.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		ds = dataSource;
		Bundler.asJavaTypes(dataSource, new PrintWriter(System.out), null);

		dao = Bundler.inflate(UserDAO.class);
	}

	@Test
	public void testSelectAll() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			dao.createDatabase();
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<User> users = dao.findAll();
			assertEquals(3, users.size());

			Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());
			assertTrue(usernames.contains("user-a"));
			assertTrue(usernames.contains("user-b"));
			assertTrue(usernames.contains("user-c"));
		}
	}
}
