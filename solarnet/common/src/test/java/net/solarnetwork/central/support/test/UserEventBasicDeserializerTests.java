/* ==================================================================
 * UserEventBasicDeserializerTests.java - 19/03/2026 7:04:07 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test cases for the {@link UserEventBasicDeserializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventBasicDeserializerTests {

	private TimeBasedV7UuidGenerator uuidGenerator;
	private ObjectMapper mapper;

	@BeforeEach
	public void setup() {
		uuidGenerator = new TimeBasedV7UuidGenerator(new SecureRandom(), Clock.systemUTC(),
				UuidUtils.V7_MICRO_COUNT_PRECISION);
		SimpleModule mod = new SimpleModule("Test");
		mod.addDeserializer(UserEvent.class, UserEventBasicDeserializer.INSTANCE);
		mapper = JsonMapper.builder().addModule(mod).build();
	}

	private String jsonValue(Object obj) {
		return mapper.writeValueAsString(obj);
	}

	@Test
	public void deserialize_typical() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final UUID eventId = uuidGenerator.generate();
		final String[] tags = new String[] { randomString(), randomString() };
		final String message = randomString();
		final String data = "{\"foo\":1}";
		final String json = """
				{
					"userId" : %d,
					"eventId": "%s",
					"tags": %s,
					"message": "%s",
					"data": %s
				}
				""".formatted(userId, eventId, jsonValue(tags), message, jsonValue(data));

		// WHEN
		UserEvent result = mapper.readValue(json, UserEvent.class);

		// THEN
		// @formatter:off
		then(result)
			.as("UserEvent parsed")
			.isNotNull()
			.as("User ID parsed")
			.returns(userId, from(UserEvent::getUserId))
			.as("Event ID parsed")
			.returns(eventId, from(UserEvent::getEventId))
			.as("Tags parsed")
			.returns(tags, from(UserEvent::getTags))
			.as("Message parsed")
			.returns(message, from(UserEvent::getMessage))
			.as("Data parsed")
			.returns(data, from(UserEvent::getData))
			;
		// @formatter:on
	}

	@Test
	public void deserialize_noData() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final UUID eventId = uuidGenerator.generate();
		final String[] tags = new String[] { randomString(), randomString() };
		final String message = randomString();
		final String json = """
				{
					"userId" : %d,
					"eventId": "%s",
					"tags": %s,
					"message": "%s"
				}
				""".formatted(userId, eventId, jsonValue(tags), message);

		// WHEN
		UserEvent result = mapper.readValue(json, UserEvent.class);

		// THEN
		// @formatter:off
		then(result)
			.as("UserEvent parsed")
			.isNotNull()
			.as("User ID parsed")
			.returns(userId, from(UserEvent::getUserId))
			.as("Event ID parsed")
			.returns(eventId, from(UserEvent::getEventId))
			.as("Tags parsed")
			.returns(tags, from(UserEvent::getTags))
			.as("Message parsed")
			.returns(message, from(UserEvent::getMessage))
			.as("Data not available")
			.returns(null, from(UserEvent::getData))
			;
		// @formatter:on
	}

	@Test
	public void deserialize_noMessage() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final UUID eventId = uuidGenerator.generate();
		final String[] tags = new String[] { randomString(), randomString() };
		final String data = "{\\\"foo\\\":1}";
		final String json = """
				{
					"userId" : %d,
					"eventId": "%s",
					"tags": %s,
					"data": %s
				}
				""".formatted(userId, eventId, jsonValue(tags), jsonValue(data));

		// WHEN
		UserEvent result = mapper.readValue(json, UserEvent.class);

		// THEN
		// @formatter:off
		then(result)
			.as("UserEvent parsed")
			.isNotNull()
			.as("User ID parsed")
			.returns(userId, from(UserEvent::getUserId))
			.as("Event ID parsed")
			.returns(eventId, from(UserEvent::getEventId))
			.as("Tags parsed")
			.returns(tags, from(UserEvent::getTags))
			.as("Message not available")
			.returns(null, from(UserEvent::getMessage))
			.as("Data parsed")
			.returns(data, from(UserEvent::getData))
			;
		// @formatter:on
	}

	@Test
	public void deserialize_noMessageOrData() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final UUID eventId = uuidGenerator.generate();
		final String[] tags = new String[] { randomString(), randomString() };
		final String json = """
				{
					"userId" : %d,
					"eventId": "%s",
					"tags": %s
				}
				""".formatted(userId, eventId, jsonValue(tags));

		// WHEN
		UserEvent result = mapper.readValue(json, UserEvent.class);

		// THEN
		// @formatter:off
		then(result)
			.as("UserEvent parsed")
			.isNotNull()
			.as("User ID parsed")
			.returns(userId, from(UserEvent::getUserId))
			.as("Event ID parsed")
			.returns(eventId, from(UserEvent::getEventId))
			.as("Tags parsed")
			.returns(tags, from(UserEvent::getTags))
			.as("Message not available")
			.returns(null, from(UserEvent::getMessage))
			.as("Data not available")
			.returns(null, from(UserEvent::getData))
			;
		// @formatter:on
	}

}
