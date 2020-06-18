/* ==================================================================
 * UserNodeEventTaskTests.java - 16/06/2020 7:24:09 am
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

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link UserNodeEventTask} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeEventTaskTests {

	@Test
	public void asMessageData_nulls() {
		// GIVEN
		UserNodeEventTask task = new UserNodeEventTask();

		// WHEN
		final String topic = "test.topic";
		Map<String, Object> msg = task.asMessageData(topic);

		// THEN
		assertThat("Message created", msg.keySet(), hasSize(5));
		assertThat("Topic", msg, hasEntry("topic", topic));
		assertThat("User ID", msg, hasEntry("userId", null));
		assertThat("Hook ID", msg, hasEntry("hookId", null));
		assertThat("Node ID", msg, hasEntry("nodeId", null));
		assertThat("Source ID", msg, hasEntry("sourceId", null));
	}

	@Test
	public void asMessageData_basic() {
		// GIVEN
		UserNodeEventTask task = new UserNodeEventTask(randomUUID(), now());
		task.setUserId(-1L);
		task.setHookId(-2L);
		task.setNodeId(-3L);
		task.setSourceId("test.source");

		// WHEN
		final String topic = "test.topic";
		Map<String, Object> msg = task.asMessageData(topic);

		// THEN
		assertThat("Message created", msg.keySet(), hasSize(5));
		assertThat("Topic", msg, hasEntry("topic", topic));
		assertThat("User ID", msg, hasEntry("userId", task.getUserId()));
		assertThat("Hook ID", msg, hasEntry("hookId", task.getHookId()));
		assertThat("Node ID", msg, hasEntry("nodeId", task.getNodeId()));
		assertThat("Source ID", msg, hasEntry("sourceId", task.getSourceId()));
	}

	@Test
	public void asMessageData_withTaskProperties() {
		// GIVEN
		UserNodeEventTask task = new UserNodeEventTask(randomUUID(), now());
		task.setUserId(-1L);
		task.setHookId(-2L);
		task.setNodeId(-3L);
		task.setSourceId("test.sourceI");

		Map<String, Object> taskProps = new LinkedHashMap<>(2);
		taskProps.put("foo", "bar");
		taskProps.put("bim", 8);
		task.setTaskProperties(taskProps);

		// WHEN
		final String topic = "test.topic";
		Map<String, Object> msg = task.asMessageData(topic);

		// THEN
		assertThat("Message created", msg.keySet(), hasSize(7));
		assertThat("Topic", msg, hasEntry("topic", topic));
		assertThat("User ID", msg, hasEntry("userId", task.getUserId()));
		assertThat("Hook ID", msg, hasEntry("hookId", task.getHookId()));
		assertThat("Node ID", msg, hasEntry("nodeId", task.getNodeId()));
		assertThat("Source ID", msg, hasEntry("sourceId", task.getSourceId()));
		assertThat("Task prop 'foo'", msg, hasEntry("foo", "bar"));
		assertThat("Task prop 'bim'", msg, hasEntry("bim", 8));
	}

	@Test
	public void asMessageData_asJson() {
		// GIVEN
		UserNodeEventTask task = new UserNodeEventTask(randomUUID(), now());
		task.setUserId(-1L);
		task.setHookId(-2L);
		task.setNodeId(-3L);
		task.setSourceId("test.source");

		Map<String, Object> taskProps = new LinkedHashMap<>(2);
		taskProps.put("ts",
				LocalDateTime.of(2020, 6, 1, 2, 3, 4, (int) TimeUnit.MILLISECONDS.toNanos(567))
						.atOffset(ZoneOffset.UTC).toInstant());
		task.setTaskProperties(taskProps);

		// WHEN
		final String topic = "test.topic";
		Map<String, Object> msg = task.asMessageData(topic);
		String json = JsonUtils.getJSONString(msg, null);

		// THEN
		assertThat("JSON created", json, equalTo(
				"{\"topic\":\"test.topic\",\"userId\":-1,\"hookId\":-2,\"nodeId\":-3,\"sourceId\":\"test.source\",\"ts\":\"2020-06-01 02:03:04.567Z\"}"));
	}
}
