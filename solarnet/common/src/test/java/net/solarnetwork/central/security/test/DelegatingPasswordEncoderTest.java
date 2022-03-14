/* ==================================================================
 * DelegatingPasswordEncoderTest.java - Mar 19, 2013 10:28:23 AM
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
import static org.junit.Assert.assertTrue;
import net.solarnetwork.central.security.DelegatingPasswordEncoder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Test case for the {@link DelegatingPasswordEncoder} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public class DelegatingPasswordEncoderTest extends AbstractJUnit4SpringContextTests {

	private static final String TEST_PASSWORD = "test.password";

	@Autowired
	public DelegatingPasswordEncoder testPasswordEncoder;

	@Test
	public void encodePassword() {
		final String encoded = testPasswordEncoder.encode(TEST_PASSWORD);
		assertNotNull(encoded);
		assertTrue(encoded.startsWith("$2a$12$"));
		assertEquals(60, encoded.length());
	}

	@Test
	public void verifyPassword() {
		final String encoded = testPasswordEncoder.encode(TEST_PASSWORD);
		assertTrue(testPasswordEncoder.isPasswordEncrypted(encoded));
		assertTrue(testPasswordEncoder.matches(TEST_PASSWORD, encoded));
	}

	@Test
	public void verifyLegacyPassword() {
		final String legacy = "{SHA}0d7defedd7a13311b2e2add2142956c53a89349b1c6fca20c6325af6f3bfd936";
		assertTrue(testPasswordEncoder.isPasswordEncrypted(legacy));
		assertTrue(testPasswordEncoder.matches(TEST_PASSWORD, legacy));
	}

}
