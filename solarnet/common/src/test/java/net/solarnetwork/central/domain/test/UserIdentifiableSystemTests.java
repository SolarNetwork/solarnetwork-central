/* ==================================================================
 * UserIdentifiableSystemTests.java - 2/10/2024 10:35:02 am
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

package net.solarnetwork.central.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserIdentifiableSystem;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link UserIdentifiableSystem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserIdentifiableSystemTests {

	private static class TestUserIdentifiableSystem implements UserIdentifiableSystem {

		private final Long userId;
		private final Object[] components;

		private TestUserIdentifiableSystem(Long userId, Object... components) {
			super();
			this.userId = userId;
			this.components = components;
		}

		@Override
		public final Long getUserId() {
			return userId;
		}

		@Override
		public String systemIdentifier() {
			return UserIdentifiableSystem.userIdSystemIdentifier(userId, components);
		}

	}

	@Test
	public void userIdSystemIdentifier_null() {
		// WHEN
		String result = UserIdentifiableSystem.userIdSystemIdentifier(null);

		// THEN
		then(result).as("Null input returns null").isNull();
		;
	}

	@Test
	public void userIdSystemIdentifier_noComponents() {
		// WHEN
		String result = UserIdentifiableSystem.userIdSystemIdentifier(123L);

		// THEN
		then(result).as("No components results in user ID with trailing delimiter").isEqualTo("123:");
	}

	@Test
	public void userIdSystemIdentifier_emptyComponents() {
		// WHEN
		String result = UserIdentifiableSystem.userIdSystemIdentifier(123L, new Object[0]);

		// THEN
		then(result).as("Empty components results in user ID with trailing delimiter").isEqualTo("123:");
	}

	@Test
	public void userIdSystemIdentifier_basic() {
		// WHEN
		String result = UserIdentifiableSystem.userIdSystemIdentifier(123L, 234L, "b");

		// THEN
		then(result).as("Components joined with delimiter").isEqualTo("123:234:b");
	}

	@Test
	public void userIdSystemIdentifier_nullComponentValues() {
		// WHEN
		String result = UserIdentifiableSystem.userIdSystemIdentifier(123L, null, null, "a", null);

		// THEN
		then(result).as("Null component values encoded as empty strings").isEqualTo("123:::a:");
	}

	@Test
	public void userIdFromSystemIdentifier_null() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void userIdFromSystemIdentifier_empty() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void userIdFromSystemIdentifier_basic() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier("123:a:b");

		// THEN
		then(result).as("First component extracted as result").isEqualTo(123L);
	}

	@Test
	public void userIdFromSystemIdentifier_noComponents() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier("123:");

		// THEN
		then(result).as("Only component extracted as result").isEqualTo(123L);
	}

	@Test
	public void userIdFromSystemIdentifier_noDelimiter() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier("123");

		// THEN
		then(result).as("Without delimiter null returned").isNull();
	}

	@Test
	public void userIdFromSystemIdentifier_notLong() {
		// WHEN
		Long result = UserIdentifiableSystem.userIdFromSystemIdentifier("a:b:c");

		// THEN
		then(result).as("Non-number component returned as null").isNull();
	}

	@Test
	public void systemIdentifierComponents_null() {
		// WHEN
		List<String> result = UserIdentifiableSystem.systemIdentifierComponents(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void systemIdentifierComponents_empty() {
		// WHEN
		List<String> result = UserIdentifiableSystem.systemIdentifierComponents("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void systemIdentifierComponents_basic() {
		// WHEN
		List<String> result = UserIdentifiableSystem.systemIdentifierComponents("a:bee:si");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly("a", "bee", "si");
	}

	@Test
	public void systemIdentifierComponents_singleton() {
		// WHEN
		List<String> result = UserIdentifiableSystem.systemIdentifierComponents("123");

		// THEN
		then(result).as("Extracted singleton where no delimiter").containsExactly("123");
	}

	@Test
	public void systemIdentifierComponents_someEmpty() {
		// WHEN
		List<String> result = UserIdentifiableSystem.systemIdentifierComponents("a::c:");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly("a", null, "c", null);
	}

	@Test
	public void systemIdentifierLongComponents_null() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents(null);

		// THEN
		then(result).as("Null input returns null").isNull();
	}

	@Test
	public void systemIdentifierLongComponents_empty() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("");

		// THEN
		then(result).as("Empty input returns null").isNull();
	}

	@Test
	public void systemIdentifierLongComponents_basic() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123:234:345");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, 234L, 345L);
	}

	@Test
	public void systemIdentifierLongComponents_singleton() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123");

		// THEN
		then(result).as("Extracted singleton where no delimiter").containsExactly(123L);
	}

	@Test
	public void systemIdentifierLongComponents_someEmpty() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123::345:");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, null, 345L, null);
	}

	@Test
	public void systemIdentifierLongComponents_someEmpty_omitNulls() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123::345:", true);

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, 345L);
	}

	@Test
	public void systemIdentifierLongComponents_someNotLong() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123:a:345:d");

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, null, 345L, null);
	}

	@Test
	public void systemIdentifierLongComponents_someNotLong_omitNulls() {
		// WHEN
		List<Long> result = UserIdentifiableSystem.systemIdentifierLongComponents("123:a:345:d", true);

		// THEN
		then(result).as("Extracted components using delimiter").containsExactly(123L, 345L);
	}

	@Test
	public void systemIdentifierForComponents() {
		// GIVEN
		final Object[] comps = new Object[] { CommonTestUtils.randomString(),
				CommonTestUtils.randomLong() };
		TestUserIdentifiableSystem sys = new TestUserIdentifiableSystem(CommonTestUtils.randomLong(),
				comps);

		// WHEN
		String result = sys.systemIdentifierForComponents(comps);

		// THEN
		then(result).as("User ID included as first component").isEqualTo("%d:%s:%d", sys.getUserId(),
				comps[0], comps[1]);
	}

}
