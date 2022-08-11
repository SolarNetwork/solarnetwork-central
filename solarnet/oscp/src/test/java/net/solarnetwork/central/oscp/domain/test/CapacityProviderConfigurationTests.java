/* ==================================================================
 * CapacityProviderConfigurationTests.java - 11/08/2022 1:57:44 pm
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

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link CapacityProviderConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityProviderConfigurationTests {

	@Test
	public void createWithUnassignedEntityId() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();

		// WHEN
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(
				UserLongPK.unassignedEntityIdKey(userId), Instant.now());

		// THEN
		assertThat("Entity key component not null", conf.getId().getEntityId(), is(notNullValue()));
		assertThat("Entity key component not assigned", conf.getId().entityIdIsAssigned(),
				is(equalTo(false)));
	}

	private void assertJson(String message, String actualJson, CapacityProviderConfiguration conf)
			throws JSONException {
		// @formatter:off
		JSONAssert.assertEquals(message, format("""
				{"userId":%d
				, "configId":%d
				, "created":"%s"
				, "name":"%s"
				, "baseUrl":"%s"
				, "registrationStatus":"%s"
				}
				""",
				conf.getUserId(),
				conf.getEntityId(),
				DateUtils.ISO_DATE_TIME_ALT_UTC.format(conf.getCreated()),
				conf.getName(),
				conf.getBaseUrl(),
				conf.getRegistrationStatus().name()
				), actualJson, true);
		// @formatter:on
	}

	@Test
	public void serializeJson() throws IOException, JSONException {
		// GIVEN
		ObjectMapper mapper = JsonUtils.newObjectMapper();

		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long confId = UUID.randomUUID().getMostSignificantBits();
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(userId, confId,
				Instant.now());
		conf.setName("Howdy");
		conf.setBaseUrl("https://localhost/" + UUID.randomUUID());
		conf.setRegistrationStatus(RegistrationStatus.Pending);

		// WHEN
		String json = mapper.writeValueAsString(conf);

		// THEN
		assertJson("JSON serialized", json, conf);
	}

}
