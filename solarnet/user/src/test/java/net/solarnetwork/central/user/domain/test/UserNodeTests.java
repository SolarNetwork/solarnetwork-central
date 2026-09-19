/* ==================================================================
 * UserNodeTests.java - 1/03/2026 10:54:18 am
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

package net.solarnetwork.central.user.domain.test;

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link UserNode} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeTests {

	@Test
	public void toJson() {
		// GIVEN
		final Long userId = randomLong();
		final String email = randomString();
		final Long nodeId = randomLong();
		final UserNode userNode = new UserNode(new User(userId, email), new SolarNode(nodeId, null));

		userNode.setId(randomLong());

		// WHEN
		String json = JsonUtils.getJSONString(userNode);

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnlyKeys("id", "idAndName", "node", "requiresAuthorization", "user", "userId")
			.contains(
				entry("id", new BigDecimal(userNode.getId())),
				entry("idAndName", nodeId.toString()),
				entry("userId", new BigDecimal(userId)),
				entry("requiresAuthorization", false)
			)
			.hasEntrySatisfying("node", node -> {
				then(node).asInstanceOf(JSON)
					.isObject()
					.containsOnly(
						entry("id", new BigDecimal(nodeId)),
						entry("created", ISO_DATE_TIME_ALT_UTC.format(userNode.getNode().getCreated()))
					)
					;
			})
			.hasEntrySatisfying("user", user -> {
				then(user).asInstanceOf(JSON)
					.isObject()
					.containsOnly(
						entry("id", new BigDecimal(userId)),
						entry("email", email)
					)
					;
			})
			;
		// @formatter:on
	}

}
