/* ==================================================================
 * CloudIntegrationsConfigurationEntityTests.java - 17/10/2024 8:52:51 am
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

package net.solarnetwork.central.c2c.domain.test;

import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;

/**
 * Test cases for the {@link CloudIntegrationsConfigurationEntity} class.
 *
 * @author matt
 * @version 1.0
 */
public class CloudIntegrationsConfigurationEntityTests {

	@Test
	public void resolvePlaceholders() {
		// GIVEN
		// @formatter:off
		var conf = new BasicIdentifiableConfiguration(null, null, Map.of(PLACEHOLDERS_SERVICE_PROPERTY, Map.of(
				"a", "eh",
				"b", "bee",
				"c", 3
				)));
		// @formatter:on

		// WHEN
		String result = CloudIntegrationsConfigurationEntity
				.resolvePlaceholders("/{a}/{b}/{c}/{d:egads}", conf);

		// THEN
		// @formatter:off
		then(result)
			.as("Placeholders resolved")
			.isEqualTo("/eh/bee/3/egads")
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholders_missing() {
		// GIVEN
		var conf = new BasicIdentifiableConfiguration(null, null, Map.of("no", "placeholders"));

		// WHEN
		String result = CloudIntegrationsConfigurationEntity.resolvePlaceholders("/{a:sad}", conf);

		// THEN
		// @formatter:off
		then(result)
			.as("Placeholders resolved even when the placeholders map is missing")
			.isEqualTo("/sad")
			;
		// @formatter:on
	}

	@Test
	public void resolvePlaceholders_notAMap() {
		// GIVEN
		var conf = new BasicIdentifiableConfiguration(null, null,
				Map.of(PLACEHOLDERS_SERVICE_PROPERTY, "oops"));

		// WHEN
		String result = CloudIntegrationsConfigurationEntity.resolvePlaceholders("/{a:sad}", conf);

		// THEN
		// @formatter:off
		then(result)
			.as("Placeholders resolved even when the placeholders service property is not a map")
			.isEqualTo("/sad")
			;
		// @formatter:on
	}

}
