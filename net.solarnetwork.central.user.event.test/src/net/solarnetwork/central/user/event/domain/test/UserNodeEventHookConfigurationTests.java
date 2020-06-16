/* ==================================================================
 * UserNodeEventHookConfigurationTests.java - 17/06/2020 11:27:53 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.Test;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link UserNodeEventHookConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeEventHookConfigurationTests {

	@Test
	public void asJson_minimumProperties() {
		// GIVEN
		Instant date = LocalDateTime.of(2020, 6, 1, 2, 3, 4).toInstant(ZoneOffset.UTC);
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(1L, 2L, date);

		// WHEN
		String json = JsonUtils.getJSONString(conf, null);

		// THEN
		assertThat("JSON generated", json,
				equalTo("{\"id\":1,\"userId\":2,\"created\":\"2020-06-01 02:03:04Z\"}"));
	}

	@Test
	public void asJson_typicalProperties() {
		// GIVEN
		Instant date = LocalDateTime.of(2020, 6, 1, 2, 3, 4).toInstant(ZoneOffset.UTC);
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(1L, 2L, date);
		conf.setName("Test");
		conf.setTopic("test");
		conf.setNodeIds(new Long[] { 1L, 2L });
		conf.setSourceIds(new String[] { "A", "B" });
		conf.setServiceIdentifier("foobar");
		conf.setServiceProps(Collections.singletonMap("foo", "bar"));

		// WHEN
		String json = JsonUtils.getJSONString(conf, null);

		// THEN
		assertThat("JSON generated", json, equalTo(
				"{\"id\":1,\"userId\":2,\"created\":\"2020-06-01 02:03:04Z\",\"name\":\"Test\",\"topic\":\"test\",\"serviceIdentifier\":\"foobar\",\"nodeIds\":[1,2],\"sourceIds\":[\"A\",\"B\"],\"serviceProperties\":{\"foo\":\"bar\"}}"));
	}

}
