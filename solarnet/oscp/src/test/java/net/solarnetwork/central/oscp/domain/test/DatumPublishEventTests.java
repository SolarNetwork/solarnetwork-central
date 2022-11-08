/* ==================================================================
 * DatumPublishEventTests.java - 10/10/2022 2:28:45 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain.test;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.domain.KeyValuePair;
import oscp.v20.GroupCapacityComplianceError;

/**
 * Test cases for the {@link DatumPublishEvent} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumPublishEventTests {

	private Long userId;
	private CapacityOptimizerConfiguration optimizer;
	private CapacityProviderConfiguration provider;
	private CapacityGroupConfiguration group;

	@BeforeEach
	public void setup() {
		userId = randomUUID().getMostSignificantBits();

		optimizer = new CapacityOptimizerConfiguration(userId, randomUUID().getMostSignificantBits(),
				now());
		optimizer.setName(randomUUID().toString());

		provider = new CapacityProviderConfiguration(userId, randomUUID().getMostSignificantBits(),
				now());
		provider.setName(randomUUID().toString());

		group = new CapacityGroupConfiguration(userId, randomUUID().getMostSignificantBits(), now());
		group.setName(randomUUID().toString());
		group.setIdentifier(randomUUID().toString());

	}

	@Test
	public void resolveSourceId() {
		// GIVEN
		OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(userId);

		UserSettings settings = new UserSettings();
		settings.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);

		DatumPublishEvent event = new DatumPublishEvent(OscpRole.CapacityOptimizer,
				GroupCapacityComplianceError.class.getSimpleName(), optimizer, provider, group, settings,
				singleton(d), (KeyValuePair[]) null);

		// WHEN
		String result = event.sourceId();
		Function<DatumPublishEvent, String> fn = DatumPublishEvent::sourceId;
		String fnResult = fn.apply(event);

		// THEN
		assertThat("Source ID resolved", result,
				is(equalTo("/oscp/co/GroupCapacityComplianceError/%d/%d/%s".formatted(
						provider.getEntityId(), optimizer.getEntityId(), group.getIdentifier()))));
		assertThat("Function resolves same", fnResult, is(equalTo(result)));
	}

	@Test
	public void resolveSourceId_actionCode() {
		// GIVEN
		OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(userId);

		UserSettings settings = new UserSettings();
		settings.setSourceIdTemplate("/oscp/{role}/{actionCode}/{cp}/{co}/{cgIdentifier}");

		DatumPublishEvent event = new DatumPublishEvent(OscpRole.CapacityOptimizer,
				GroupCapacityComplianceError.class.getSimpleName(), optimizer, provider, group, settings,
				singleton(d), (KeyValuePair[]) null);

		// WHEN
		String result = event.sourceId();
		Function<DatumPublishEvent, String> fn = DatumPublishEvent::sourceId;
		String fnResult = fn.apply(event);

		// THEN
		assertThat("Source ID resolved", result, is(equalTo("/oscp/co/gcce/%d/%d/%s"
				.formatted(provider.getEntityId(), optimizer.getEntityId(), group.getIdentifier()))));
		assertThat("Function resolves same", fnResult, is(equalTo(result)));
	}

}
