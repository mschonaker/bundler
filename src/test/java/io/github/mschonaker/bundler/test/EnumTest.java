package io.github.mschonaker.bundler.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Bundler.Transaction;
import io.github.mschonaker.bundler.Config;
import io.github.mschonaker.bundler.test.daos.enums.EnumService;
import io.github.mschonaker.bundler.test.daos.enums.SimpleEnum;

public class EnumTest {

	private static DataSource ds;

	private static EnumService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		JdbcDataSource dataSource = new JdbcDataSource();

		dataSource.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		ds = dataSource;

		service = Bundler.inflate(EnumService.class, new Config().lenient());
		try (Transaction tx = Bundler.writeTransaction(dataSource)) {
			service.createTables();
			tx.success();
		}
	}

	@After
	public void after() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.empty();
			tx.success();
		}
	}

	@Test
	public void testStrings() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new String[] { "A", "B", "C" }), service.listStrings());
		}
	}

	@Test
	public void testIntegers() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new Integer[] { 0, 1, 2 }), service.listIntegers());
		}
	}

	@Test
	public void testEnumAsStrings() {

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new SimpleEnum[] { SimpleEnum.A, SimpleEnum.B, SimpleEnum.C }),
					service.listEnumsAsStrings());
		}
	}

	@Test
	public void testEnumAsInteger() {

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.C);
			service.insert(SimpleEnum.B);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new SimpleEnum[] { SimpleEnum.A, SimpleEnum.C, SimpleEnum.B }),
					service.listEnumsAsIntegers());
		}
	}
}
