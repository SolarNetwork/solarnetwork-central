/* ==================================================================
 * UserIdRelatedTests.java - 8/03/2026 10:58:53 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link UserIdRelated} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserIdRelatedTests {

	private static class TestUserIdRelated implements UserIdRelated {

		private final Long userId;

		private TestUserIdRelated(Long userId) {
			super();
			this.userId = userId;
		}

		@Override
		public Long getUserId() throws IllegalStateException {
			return userId;
		}

	}

	@Test
	public void userIdIsAssigned() {
		// GIVEN
		final var userId = CommonTestUtils.randomLong();

		// WHEN
		final var o = new TestUserIdRelated(userId);

		// THEN
		then(o.userIdIsAssigned()).as("Non-min user ID is assigned").isTrue();
		then(o.assignedUserId()).as("Assigned user ID returned").isSameAs(userId);
	}

	@Test
	public void userIdIsAssigned_null() {
		// GIVEN
		final Long userId = null;

		// WHEN
		final var o = new TestUserIdRelated(userId);

		// THEN
		then(o.userIdIsAssigned()).as("Null user ID is not assigned").isFalse();
		then(o.assignedUserId()).as("Unassigned user ID null").isNull();
	}

	@Test
	public void userIdIsAssigned_unassigned() {
		// GIVEN
		final var userId = UserIdRelated.UNASSIGNED_USER_ID;

		// WHEN
		final var o = new TestUserIdRelated(userId);

		// THEN
		then(o.userIdIsAssigned()).as("Unassigned constant user ID is not assigned").isFalse();
		then(o.assignedUserId()).as("Unassigned user ID null").isNull();
	}

	@Test
	public void userIdIsAssigned_nonConstant() {
		// GIVEN
		final var userId = Long.valueOf(UserIdRelated.UNASSIGNED_USER_ID + 1L - 1L);

		// WHEN
		final var o = new TestUserIdRelated(userId);

		// THEN
		then(o.userIdIsAssigned()).as("Non-constant unassigned value user ID is assigned").isTrue();
		then(o.assignedUserId()).as("Assigned user ID returned").isSameAs(userId);
	}

}
