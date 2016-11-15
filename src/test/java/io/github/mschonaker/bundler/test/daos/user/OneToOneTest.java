package io.github.mschonaker.bundler.test.daos.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Bundler.Transaction;

public class OneToOneTest {

	private static DataSource ds;
	private static UserService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		JdbcDataSource dataSource = new JdbcDataSource();

		dataSource.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		ds = dataSource;

		service = Bundler.inflate(UserService.class);
		try (Transaction tx = Bundler.writeTransaction(dataSource)) {
			service.createTables();
			tx.success();
		}

		Bundler.dumpDB(dataSource, new PrintWriter(System.out), null);
	}

	@Test
	public void baseline() {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user = service.getUserWithPreferences("alpha");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertTrue(user.getPreferences().isA());
			assertTrue(user.getPreferences().getB());

			user = service.getUserWithPreferences("beta");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertFalse(user.getPreferences().isA());
			assertFalse(user.getPreferences().getB());

		}

	}

	@Test
	public void oneShotOneToOne() {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user = service.getUserWithPreferencesOneShot("alpha");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertTrue(user.getPreferences().isA());
			assertTrue(user.getPreferences().getB());

			user = service.getUserWithPreferencesOneShot("beta");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertFalse(user.getPreferences().isA());
			assertFalse(user.getPreferences().getB());

		}

	}

	@Test
	public void hybridOneToOne() {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user = service.getUserWithPreferencesHybrid("alpha");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertTrue(user.getPreferences().getB());
			assertTrue(user.getPreferences().isA());

			user = service.getUserWithPreferencesHybrid("beta");
			assertNotNull(user);
			assertNotNull(user.getPreferences());
			assertFalse(user.getPreferences().getB());
			assertFalse(user.getPreferences().isA());

		}

	}
}
