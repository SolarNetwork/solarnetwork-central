/* ==================================================================
 * StaticFluxPublishSettingsDaoTests.java - 26/06/2024 3:52:10 pm
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

package net.solarnetwork.central.datum.flux.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.flux.dao.StaticFluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettingsInfo;

/**
 * Test cases for the {@link StaticFluxPublishSettingsDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class StaticFluxPublishSettingsDaoTests {

	@Test
	public void access() {
		// GIVEN
		FluxPublishSettingsInfo settings = new FluxPublishSettingsInfo(true, true);

		StaticFluxPublishSettingsDao dao = new StaticFluxPublishSettingsDao(settings);

		// WHEN
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(1L, 2L, "a");

		// THEN
		then(result).as("Static result provided").isSameAs(settings);
	}

}
