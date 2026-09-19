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

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.security.LegacyPasswordEncoder;

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

	@BeforeEach
	public void setup() {
		encoder = new LegacyPasswordEncoder();
	}

	@Test
	public void encryptPassword() {
		String encoded = encoder.encode(TEST_PASSWORD);
		then(encoded).isEqualTo("{SHA}0d7defedd7a13311b2e2add2142956c53a89349b1c6fca20c6325af6f3bfd936");
	}

	@Test
	public void encryptNull() {
		String encoded = encoder.encode(null);
		then(encoded).isNull();
	}

	@Test
	public void matches() {
		String encoded = encoder.encode(TEST_PASSWORD);
		then(encoder.matches(TEST_PASSWORD, encoded)).isTrue();
	}

	@Test
	public void matchesNull() {
		then(encoder.matches(null, null)).isFalse();
		then(encoder.matches(null, "foo")).isFalse();
		then(encoder.matches("foo", null)).isFalse();
	}

	@Test
	public void doesNotMatch() {
		then(encoder.matches("nope", encoder.encode("test.password"))).isFalse();
	}
}
