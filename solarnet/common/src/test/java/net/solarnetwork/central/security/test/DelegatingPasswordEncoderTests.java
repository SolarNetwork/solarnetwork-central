/* ==================================================================
 * DelegatingPasswordEncoderTests.java - Mar 19, 2013 10:28:23 AM
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import net.solarnetwork.central.security.DelegatingPasswordEncoder;

/**
 * Test case for the {@link DelegatingPasswordEncoder} class.
 * 
 * @author matt
 * @version 1.0
 */
@SpringJUnitConfig
@ContextConfiguration
public class DelegatingPasswordEncoderTests {

	private static final String TEST_PASSWORD = "test.password";

	@Autowired
	public DelegatingPasswordEncoder testPasswordEncoder;

	@Test
	public void encodePassword() {
		final String encoded = testPasswordEncoder.encode(TEST_PASSWORD);
		then(encoded).isNotNull().startsWith("$2a$12$").hasSize(60);
	}

	@Test
	public void verifyPassword() {
		final String encoded = testPasswordEncoder.encode(TEST_PASSWORD);
		then(testPasswordEncoder.isPasswordEncrypted(encoded)).isTrue();
		then(testPasswordEncoder.matches(TEST_PASSWORD, encoded)).isTrue();
	}

	@Test
	public void verifyLegacyPassword() {
		final String legacy = "{SHA}0d7defedd7a13311b2e2add2142956c53a89349b1c6fca20c6325af6f3bfd936";
		then(testPasswordEncoder.isPasswordEncrypted(legacy)).isTrue();
		then(testPasswordEncoder.matches(TEST_PASSWORD, legacy)).isTrue();
	}

}
