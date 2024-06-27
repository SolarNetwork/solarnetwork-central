/* ==================================================================
 * UserFluxDefaultAggregatePublishConfigurationInputTests.java - 26/06/2024 7:21:57 am
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;

/**
 * Test cases for the {@link UserFluxDefaultAggregatePublishConfigurationInput}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserFluxDefaultAggregatePublishConfigurationInputTests {

	@Test
	public void toEntity() {
		// GIVEN
		UserFluxDefaultAggregatePublishConfigurationInput input = new UserFluxDefaultAggregatePublishConfigurationInput();
		input.setPublish(true);
		input.setRetain(true);

		// WHEN
		var id = randomLong();
		var conf = input.toEntity(id, Instant.now());

		// THEN
		// @formatter:off
		then(conf)
			.as("Configuration created")
			.isNotNull()
			.as("ID copied")
			.returns(id, from(UserFluxDefaultAggregatePublishConfiguration::getId))
			.as("Publish flag copied")
			.returns(input.isPublish(), from(UserFluxDefaultAggregatePublishConfiguration::isPublish))
			.as("Retain flag copied")
			.returns(input.isRetain(), from(UserFluxDefaultAggregatePublishConfiguration::isRetain))
			;		
		// @formatter:on
	}

}
