package io.github.mschonaker.bundler.test.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.github.mschonaker.bundler.loader.Bundle;
import io.github.mschonaker.bundler.loader.BundleLoader;

public class LoaderMixInTest {

	@Test
	public void testReplace() throws Exception {

		String a = "<bundler><first>SELECT 1</first></bundler>";
		String b = "<bundler><first>SELECT 2</first></bundler>";

		Map<String, Bundle> bundles = new HashMap<>();

		BundleLoader.load(bundles, new StringReader(a));

		assertEquals(1, bundles.size());
		assertTrue(bundles.containsKey("first"));
		assertEquals("SELECT 1", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);

		BundleLoader.load(bundles, new StringReader(b));

		assertEquals(1, bundles.size());
		assertTrue(bundles.containsKey("first"));
		assertEquals("SELECT 2", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);
	}

	@Test
	public void testPreserved() throws Exception {

		String a = "<bundler><first>SELECT 1</first><preserved>SELECT 10</preserved></bundler>";
		String b = "<bundler><first>SELECT 2</first></bundler>";

		Map<String, Bundle> bundles = new HashMap<>();

		BundleLoader.load(bundles, new StringReader(a));

		assertEquals(2, bundles.size());
		assertTrue(bundles.containsKey("first"));
		assertTrue(bundles.containsKey("preserved"));

		assertEquals("SELECT 1", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);

		assertEquals("SELECT 10", bundles.get("preserved").sql);
		assertNull(bundles.get("preserved").children);
		assertNull(bundles.get("preserved").expressions);
		assertEquals("preserved", bundles.get("preserved").name);

		BundleLoader.load(bundles, new StringReader(b));

		assertEquals("SELECT 2", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);

		assertEquals("SELECT 10", bundles.get("preserved").sql);
		assertNull(bundles.get("preserved").children);
		assertNull(bundles.get("preserved").expressions);
		assertEquals("preserved", bundles.get("preserved").name);
	}

	@Test
	public void testAdded() throws Exception {

		String a = "<bundler><first>SELECT 1</first></bundler>";
		String b = "<bundler><added>SELECT 2</added></bundler>";

		Map<String, Bundle> bundles = new HashMap<>();

		BundleLoader.load(bundles, new StringReader(a));

		assertEquals(1, bundles.size());
		assertTrue(bundles.containsKey("first"));
		assertEquals("SELECT 1", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);

		BundleLoader.load(bundles, new StringReader(b));

		assertEquals("SELECT 1", bundles.get("first").sql);
		assertNull(bundles.get("first").children);
		assertNull(bundles.get("first").expressions);
		assertEquals("first", bundles.get("first").name);

		assertEquals("SELECT 2", bundles.get("added").sql);
		assertNull(bundles.get("added").children);
		assertNull(bundles.get("added").expressions);
		assertEquals("added", bundles.get("added").name);
	}

}
