/* ==================================================================
 * CloudDatumStreamMappingConfigurationTests.java - 16/10/2024 8:09:23 am
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link CloudDatumStreamMappingConfiguration} class.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamMappingConfigurationTests {

	@Test
	public void toJson() {
		// GIVEN
		CloudDatumStreamMappingConfiguration entity = new CloudDatumStreamMappingConfiguration(
				randomLong(), randomLong(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		entity.setModified(entity.getCreated().plusSeconds(1));
		entity.setServiceProps(Map.of("foo", "bar"));
		entity.setName(randomString());
		entity.setIntegrationId(randomLong());

		// WHEN
		String json = JsonUtils.getJSONString(entity);

		// THEN
		// @formatter:off
		then(json)
			.as("JSON formatted")
			.isEqualToIgnoringWhitespace("""
				{
					"userId":%d,
					"configId":%d,
					"created":"%s",
					"modified":"%s",
					"name":"%s",
					"integrationId":%d,
					"serviceProperties":{
						"foo":"bar"
					}
				}
				""".formatted(
						entity.getUserId(),
						entity.getConfigId(),
						DateUtils.ISO_DATE_TIME_ALT_UTC.format(entity.getCreated()),
						DateUtils.ISO_DATE_TIME_ALT_UTC.format(entity.getModified()),
						entity.getName(),
						entity.getIntegrationId()
					))
			;
		// @formatter:on
	}

}
