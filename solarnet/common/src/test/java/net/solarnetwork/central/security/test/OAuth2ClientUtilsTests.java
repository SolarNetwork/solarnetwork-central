/* ==================================================================
 * OAuth2ClientUtilsTests.java - 22/09/2024 11:49:27 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.security.OAuth2ClientUtils;

/**
 * Test cases for the {@link OAuth2ClientUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OAuth2ClientUtilsTests {

	@Test
	public void userIdClientRegistrationId_null() {
		// WHEN
		String result = OAuth2ClientUtils.userIdClientRegistrationId(null);

		// THEN
		then(result).as("Null input returns null").isNull();
		;
	}

	@Test
	public void userIdClientRegistrationId_noComponents() {
		// WHEN
		String result = OAuth2ClientUtils.userIdClientRegistrationId(123L);

		// THEN
		then(result).as("No components results in user ID with trailing delimiter").isEqualTo("123:");
	}

	@Test
	public void userIdClientRegistrationId_emptyComponents() {
		// WHEN
		String result = OAuth2ClientUtils.userIdClientRegistrationId(123L, new Object[0]);

		// THEN
		then(result).as("Empty components results in user ID with trailing delimiter").isEqualTo("123:");
	}

	@Test
	public void userIdClientRegistrationId_basic() {
		// WHEN
		String result = OAuth2ClientUtils.userIdClientRegistrationId(123L, 234L, "b");

		// THEN
		then(result).as("Components joined with delimiter").isEqualTo("123:234:b");
	}

	@Test
	public void userIdClientRegistrationId_nullComponentValues() {
		// WHEN
		String result = OAuth2ClientUtils.userIdClientRegistrationId(123L, null, null, "a", null);

		// THEN
		then(result).as("Null component values encoded as empty strings").isEqualTo("123:::a:");
	}

	@Test
	public void userIdFromClientRegistrationId_null() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void userIdFromClientRegistrationId_empty() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void userIdFromClientRegistrationId_basic() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId("123:a:b");

		// THEN
		then(result).as("First component extracted as result").isEqualTo(123L);
	}

	@Test
	public void userIdFromClientRegistrationId_noComponents() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId("123:");

		// THEN
		then(result).as("Only component extracted as result").isEqualTo(123L);
	}

	@Test
	public void userIdFromClientRegistrationId_noDelimiter() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId("123");

		// THEN
		then(result).as("Without delimiter null returned").isNull();
	}

	@Test
	public void userIdFromClientRegistrationId_notLong() {
		// WHEN
		Long result = OAuth2ClientUtils.userIdFromClientRegistrationId("a:b:c");

		// THEN
		then(result).as("Non-number component returned as null").isNull();
	}

	@Test
	public void clientRegistrationIdComponents_null() {
		// WHEN
		List<String> result = OAuth2ClientUtils.clientRegistrationIdComponents(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void clientRegistrationIdComponents_empty() {
		// WHEN
		List<String> result = OAuth2ClientUtils.clientRegistrationIdComponents("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void clientRegistrationIdComponents_basic() {
		// WHEN
		List<String> result = OAuth2ClientUtils.clientRegistrationIdComponents("a:bee:si");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly("a", "bee", "si");
	}

	@Test
	public void clientRegistrationIdComponents_singleton() {
		// WHEN
		List<String> result = OAuth2ClientUtils.clientRegistrationIdComponents("123");

		// THEN
		then(result).as("Extracted singleton where no delimiter").containsExactly("123");
	}

	@Test
	public void clientRegistrationIdComponents_someEmpty() {
		// WHEN
		List<String> result = OAuth2ClientUtils.clientRegistrationIdComponents("a::c:");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly("a", null, "c", null);
	}

	@Test
	public void clientRegistrationIdLongComponents_null() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void clientRegistrationIdLongComponents_empty() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void clientRegistrationIdLongComponents_basic() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents("123:234:345");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, 234L, 345L);
	}

	@Test
	public void clientRegistrationIdLongComponents_singleton() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents("123");

		// THEN
		then(result).as("Extracted singleton where no delimiter").containsExactly(123L);
	}

	@Test
	public void clientRegistrationIdLongComponents_someEmpty() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents("123::345:");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, null, 345L, null);
	}

	@Test
	public void clientRegistrationIdLongComponents_someNotLong() {
		// WHEN
		List<Long> result = OAuth2ClientUtils.clientRegistrationIdLongComponents("123:a:345:d");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, null, 345L, null);
	}

}
