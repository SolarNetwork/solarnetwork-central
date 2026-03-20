/* ==================================================================
 * UserEventSerializerTests.java - 1/08/2022 11:05:47 am
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

package net.solarnetwork.central.support.test;

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidUtils;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * Test cases for the {@link UserEventSerializer}.
 * 
 * @author matt
 * @version 1.1
 */
public class UserEventSerializerTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MICROSECONDS.toNanos(234567))
			.toInstant(ZoneOffset.UTC);
	private static final String TEST_DATE_STRING = "2021-08-11 16:45:01.234567Z";

	private TimeBasedV7UuidGenerator uuidGenerator;
	private JsonMapper mapper;
	private CBORMapper cborMapper;

	@BeforeEach
	public void setup() {
		uuidGenerator = new TimeBasedV7UuidGenerator(new SecureRandom(),
				Clock.fixed(TEST_DATE, ZoneOffset.UTC), UuidUtils.V7_MICRO_COUNT_PRECISION);
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		mapper = JsonUtils.JSON_OBJECT_MAPPER.rebuild().addModule(mod).build();
		cborMapper = CborUtils.CBOR_OBJECT_MAPPER.rebuild().addModule(mod).build();
	}

	@Test
	public void json_typical() throws IOException {
		// GIVEN
		UserEvent event = new UserEvent(randomLong(), uuidGenerator.generate(),
				new String[] { randomString(), randomString() }, randomString(), "{\"foo\":1}");

		// WHEN
		String json = mapper.writeValueAsString(event);

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("userId", event.getUserId()),
				entry("created", TEST_DATE_STRING),
				entry("eventId", event.getEventId().toString()),
				entry("tags", event.getTags()),
				entry("message", event.getMessage()),
				entry("data",  JsonUtils.getStringMap(event.getData()))
			)
			;
		// @formatter:on
	}

	@Test
	public void cbor_typical() throws IOException {
		// GIVEN
		UserEvent event = new UserEvent(randomLong(), uuidGenerator.generate(),
				new String[] { randomString(), randomString() }, randomString(), "{\"foo\":1}");

		// WHEN
		byte[] cbor = cborMapper.writeValueAsBytes(event);
		String json = mapper.writeValueAsString(cborMapper.readTree(cbor));

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("userId", event.getUserId()),
				entry("created", TEST_DATE_STRING),
				entry("eventId", event.getEventId().toString()),
				entry("tags", event.getTags()),
				entry("message", event.getMessage()),
				entry("data",  JsonUtils.getStringMap(event.getData()))
			)
			;
		// @formatter:on
	}

	@Test
	public void json_noMessageOrData() throws IOException {
		// GIVEN
		UserEvent event = new UserEvent(randomLong(), uuidGenerator.generate(),
				new String[] { randomString(), randomString() }, null, null);

		// WHEN
		String json = mapper.writeValueAsString(event);

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("userId", event.getUserId()),
				entry("created", TEST_DATE_STRING),
				entry("eventId", event.getEventId().toString()),
				entry("tags", event.getTags())
			)
			;
		// @formatter:on
	}

	@Test
	public void json_null() throws IOException {
		// GIVEN

		// WHEN
		String json = mapper.writeValueAsString((UserEvent) null);

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isNull()
			;
		// @formatter:on
	}

}
