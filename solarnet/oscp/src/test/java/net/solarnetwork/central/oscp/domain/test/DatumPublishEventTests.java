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
import static net.solarnetwork.central.datum.domain.GeneralObjectDatumKey.UNASSIGNED_OBJECT_ID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
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
		userId = randomLong();

		optimizer = new CapacityOptimizerConfiguration(userId, randomLong(), now(), randomString(),
				randomLong(), RegistrationStatus.Pending);
		optimizer.setName(randomString());

		provider = new CapacityProviderConfiguration(userId, randomLong(), now(), randomString(),
				randomLong(), RegistrationStatus.Pending);
		provider.setName(randomString());

		group = new CapacityGroupConfiguration(userId, randomLong(), now(), randomString(),
				randomString(), provider.getConfigId(), optimizer.getConfigId(),
				MeasurementPeriod.FifteenMinute, MeasurementPeriod.FifteenMinute);
	}

	@Test
	public void resolveSourceId() {
		// GIVEN
		OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(
				new GeneralNodeDatumPK(UNASSIGNED_OBJECT_ID, now(), ""), userId);

		UserSettings settings = new UserSettings(randomLong(), now(), randomLong());
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
		OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(
				new GeneralNodeDatumPK(UNASSIGNED_OBJECT_ID, now(), ""), userId);

		UserSettings settings = new UserSettings(randomLong(), now(), randomLong());
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
