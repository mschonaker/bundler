package io.github.mschonaker.bundler.test.daos.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import io.github.mschonaker.bundler.Bundler;
import io.github.mschonaker.bundler.Bundler.Transaction;
import io.github.mschonaker.bundler.Config;
import io.github.mschonaker.bundler.test.AbstractTest;

public class CRUDTest extends AbstractTest {

	private static CRUDService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		service = initialize(CRUDService.class, new Config().strict());

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.createTables();
			tx.success();
		}
	}

	@Test
	public void testCRUD() throws Exception {

		User a, b;
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			assertEquals(0, service.count(), 0);
			a = service.find("a");
			assertNull(a);

			a = new User("a", "secret", "User A");

			service.upsert(a);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			b = service.find("a");
			assertNotNull(b);
			assertEquals(a, b);
			assertEquals(1, service.count(), 0);
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			a.setPassword("top secret");
			service.upsert(a);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			b = service.find("a");
			assertNotNull(b);
			assertEquals("top secret", b.getPassword());
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.delete("a");
			assertEquals(0, service.count(), 0);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(0, service.count(), 0);
		}
	}
}
