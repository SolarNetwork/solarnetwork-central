/* ==================================================================
 * CapacityOptimizerConfigurationTests.java - 14/08/2022 7:31:03 am
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
import java.util.Collections;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link CapacityOptimizerConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityOptimizerConfigurationTests {

	@Test
	public void createWithUnassignedEntityId() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();

		// WHEN
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), Instant.now());

		// THEN
		assertThat("Entity key component not null", conf.getId().getEntityId(), is(notNullValue()));
		assertThat("Entity key component not assigned", conf.getId().entityIdIsAssigned(),
				is(equalTo(false)));
	}

	private void assertJson(String message, String actualJson, CapacityOptimizerConfiguration conf)
			throws JSONException {
		// @formatter:off
		JSONAssert.assertEquals(message, format("""
				{"userId":%d
				, "configId":%d
				, "created":"%s"
				, "modified":"%s"
				, "enabled":%s
				, "name":"%s"
				, "baseUrl":"%s"
				, "registrationStatus":"%s"
				, "publishToSolarIn":%s
				, "publishToSolarFlux":%s
				, "sourceIdTemplate":"%s"
				, "serviceProps":%s
				}
				""",
				conf.getUserId(),
				conf.getEntityId(),
				DateUtils.ISO_DATE_TIME_ALT_UTC.format(conf.getCreated()),
				DateUtils.ISO_DATE_TIME_ALT_UTC.format(conf.getModified()),
				conf.isEnabled(),
				conf.getName(),
				conf.getBaseUrl(),
				conf.getRegistrationStatus().name(),
				conf.isPublishToSolarIn(),
				conf.isPublishToSolarFlux(),
				conf.getSourceIdTemplate(),
				JsonUtils.getJSONString(conf.getServiceProps(), "null")
				), actualJson, true);
		// @formatter:on
	}

	@Test
	public void serializeJson() throws IOException, JSONException {
		// GIVEN
		ObjectMapper mapper = JsonUtils.newObjectMapper();

		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long confId = UUID.randomUUID().getMostSignificantBits();
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(userId, confId,
				Instant.now());
		conf.setModified(conf.getCreated().plusSeconds(9));
		conf.setEnabled(true);
		conf.setName("Howdy");
		conf.setBaseUrl("https://localhost/" + UUID.randomUUID());
		conf.setRegistrationStatus(RegistrationStatus.Pending);
		conf.setPublishToSolarIn(true);
		conf.setPublishToSolarFlux(false);
		conf.setSourceIdTemplate("foo/bar");
		conf.setServiceProps(Collections.singletonMap("foo", "bar"));

		// WHEN
		String json = mapper.writeValueAsString(conf);

		// THEN
		assertJson("JSON serialized", json, conf);
	}

}
