/* ==================================================================
 * UserFluxAggregatePublishConfigurationInputTests.java - 26/06/2024 7:21:57 am
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

package net.solarnetwork.central.user.flux.domain.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfigurationInput;

/**
 * Test cases for the {@link UserFluxAggregatePublishConfigurationInput} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserFluxAggregatePublishConfigurationInputTests {

	@Test
	public void toEntity() {
		// GIVEN
		UserFluxAggregatePublishConfigurationInput input = new UserFluxAggregatePublishConfigurationInput();
		input.setNodeIds(new Long[] { 1L, 2L });
		input.setPublish(true);
		input.setRetain(true);
		input.setSourceIds(new String[] { "a", "b" });

		// WHEN
		var id = unassignedEntityIdKey(randomLong());
		var conf = input.toEntity(id, Instant.now());

		// THEN
		// @formatter:off
		then(conf)
			.as("Configuration created")
			.isNotNull()
			.as("ID copied")
			.returns(id, from(UserFluxAggregatePublishConfiguration::getId))
			.as("Publish flag copied")
			.returns(input.isPublish(), from(UserFluxAggregatePublishConfiguration::isPublish))
			.as("Retain flag copied")
			.returns(input.isRetain(), from(UserFluxAggregatePublishConfiguration::isRetain))
			.satisfies(c -> {
				then(c.getNodeIds())
					.as("Node IDs are sorted")
					.containsExactly(input.getNodeIds())
					;
				then(c.getSourceIds())
					.as("Node IDs are sorted")
					.containsExactly(input.getSourceIds())
					;
			})
			;		
		// @formatter:on
	}

	@Test
	public void toEntity_sortNodeIds() {
		// GIVEN
		UserFluxAggregatePublishConfigurationInput input = new UserFluxAggregatePublishConfigurationInput();
		input.setNodeIds(new Long[] { 3L, 5L, 2L });

		// WHEN
		var id = unassignedEntityIdKey(randomLong());
		var conf = input.toEntity(id, Instant.now());

		// THEN
		// @formatter:off
		then(conf)
			.as("Configuration created")
			.isNotNull()
			.as("ID copied")
			.returns(id, from(UserFluxAggregatePublishConfiguration::getId))
			.satisfies(c -> {
				then(c.getNodeIds())
					.as("Node IDs are sorted")
					.containsExactly(2L, 3L, 5L)
					;
			})
			;		
		// @formatter:on
	}

	@Test
	public void toEntity_sortSourceIds() {
		// GIVEN
		UserFluxAggregatePublishConfigurationInput input = new UserFluxAggregatePublishConfigurationInput();
		input.setSourceIds(new String[] { "c", "e", "b" });

		// WHEN
		var id = unassignedEntityIdKey(randomLong());
		var conf = input.toEntity(id, Instant.now());

		// THEN
		// @formatter:off
		then(conf)
			.as("Configuration created")
			.isNotNull()
			.as("ID copied")
			.returns(id, from(UserFluxAggregatePublishConfiguration::getId))
			.satisfies(c -> {
				then(c.getSourceIds())
					.as("Source IDs are sorted")
					.containsExactly("b", "c", "e")
					;
			})
			;		
		// @formatter:on
	}

}
