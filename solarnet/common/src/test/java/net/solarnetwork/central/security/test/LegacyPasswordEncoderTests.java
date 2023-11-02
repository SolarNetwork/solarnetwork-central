/* ==================================================================
 * LegacyPasswordEncoderTests.java - Mar 19, 2013 10:20:48 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.security.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.security.LegacyPasswordEncoder;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for the {@link LegacyPasswordEncoder} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class LegacyPasswordEncoderTests {

	private static final String TEST_PASSWORD = "test.password";

	private LegacyPasswordEncoder encoder;

	@Before
	public void setup() {
		encoder = new LegacyPasswordEncoder();
	}

	@Test
	public void encryptPassword() {
		String encoded = encoder.encode(TEST_PASSWORD);
		assertNotNull(encoded);
		assertEquals("{SHA}0d7defedd7a13311b2e2add2142956c53a89349b1c6fca20c6325af6f3bfd936", encoded);
	}

	@Test
	public void encryptNull() {
		String encoded = encoder.encode(null);
		assertNull(encoded);
	}

	@Test
	public void matches() {
		String encoded = encoder.encode(TEST_PASSWORD);
		assertEquals(true, encoder.matches(TEST_PASSWORD, encoded));
	}

	@Test
	public void matchesNull() {
		assertEquals(false, encoder.matches(null, null));
		assertEquals(false, encoder.matches(null, "foo"));
		assertEquals(false, encoder.matches("foo", null));
	}

	@Test
	public void doesNotMatch() {
		assertEquals(false, encoder.matches("nope", encoder.encode("test.password")));
	}
}
