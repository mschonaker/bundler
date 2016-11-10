package io.github.mschonaker.bundler.test.coercions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.mschonaker.bundler.utils.Coercions;

public class JRECoercionsTest {

	@Test
	public void toStrings() {
		assertEquals("16", Coercions.JRE.coerce(16, String.class));
		assertEquals("C", Coercions.JRE.coerce('C', String.class));
		assertEquals(false, Coercions.JRE.coerce(0, Boolean.class));
		assertEquals(true, Coercions.JRE.coerce(-16, Boolean.class));
	}

	static enum TEST {
		A, B, CD
	}

	@Test
	public void toEnum() {

		assertEquals(TEST.CD, Coercions.JRE.coerce("CD", TEST.class));
		assertEquals(TEST.B, Coercions.JRE.coerce(1, TEST.class));
		assertEquals("CD", Coercions.JRE.coerce(TEST.CD, String.class));
		assertEquals(1, Coercions.JRE.coerce(TEST.B, Integer.class), 0);

	}

	@Test
	public void toCustomEnum() {

		Coercions my = new Coercions(Coercions.JRE)//
				.add(String.class, TEST.class, (o, t) -> TEST.valueOf(o));

		assertEquals(TEST.CD, my.coerce("CD", TEST.class));
	}

}
