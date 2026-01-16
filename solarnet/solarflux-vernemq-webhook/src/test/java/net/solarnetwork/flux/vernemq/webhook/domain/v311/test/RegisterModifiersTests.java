/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.domain.v311.test;

import static net.solarnetwork.flux.vernemq.webhook.support.JsonUtils.JSON_MAPPER;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterModifiers;

/**
 * Test cases for the {@link RegisterModifiers} class.
 * 
 * @author matt
 */
public class RegisterModifiersTests {

	@Test
	public void jsonFull() throws JSONException {
		RegisterModifiers mods = RegisterModifiers.builder().withCleanSession(true)
				.withMaxInflightMessages(1).withMaxMessageRate(2).withMaxMessageSize(3)
				.withRegView("foo").withRetryInterval(4L).withSubscriberId("bar").withUpgradeQos(true)
				.build();

		String json = JSON_MAPPER.writeValueAsString(mods);
		JSONAssert.assertEquals(
				"""
						{
						  "clean_session": %s,
						  "max_message_rate": %d,
						  "max_message_size": %d,
						  "max_inflight_messages": %d,
						  "reg_view": "%s",
						  "retry_interval": %d,
						  "subscriber_id": "%s",
						  "upgrade_qos": %s
						}
						""".formatted(mods.getCleanSession(), mods.getMaxMessageRate(),
						mods.getMaxMessageSize(), mods.getMaxInflightMessages(), mods.getRegView(),
						mods.getRetryInterval(), mods.getSubscriberId(), mods.getUpgradeQos()),
				json, true);
	}

	@Test
	public void jsonSome() throws JSONException {
		RegisterModifiers mods = RegisterModifiers.builder().withMaxMessageSize(1)
				.withMaxInflightMessages(2).withRetryInterval(3L).build();

		String json = JSON_MAPPER.writeValueAsString(mods);
		JSONAssert.assertEquals("""
				{
				  "max_message_size": %d,
				  "max_inflight_messages": %d,
				  "retry_interval": %d
				}
				""".formatted(mods.getMaxMessageSize(), mods.getMaxInflightMessages(),
				mods.getRetryInterval()), json, true);
	}

}
