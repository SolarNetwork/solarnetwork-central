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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link UserEventSerializer}.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventSerializerTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MILLISECONDS.toNanos(234))
			.toInstant(ZoneOffset.UTC);
	private static final String TEST_DATE_STRING = "2021-08-11 16:45:01.234Z";

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = JsonUtils.newObjectMapper();
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		m.registerModule(mod);
		return m;
	}

	@Before
	public void setup() {
		mapper = createObjectMapper();
	}

	@Test
	public void serialize_typical() throws IOException {
		// GIVEN
		UserEvent event = new UserEvent(1L, TEST_DATE, UUID.randomUUID(), "foo", "test", "{\"foo\":1}");

		// WHEN
		String json = mapper.writeValueAsString(event);

		// THEN
		// @formatter:off
		assertThat("JSON", json,
				is(equalTo("{\"userId\":" + event.getUserId()						
						+ ",\"created\":\"" + TEST_DATE_STRING + "\""
						+ ",\"eventId\":\"" + event.getEventId() + "\""
						+ ",\"kind\":\"" + event.getKind() + "\""
						+ ",\"message\":\"" + event.getMessage() + "\""
						+ ",\"data\":" + event.getData()
						+ "}")));
		// @formatter:on
	}

	@Test
	public void serialize_noMessageOrData() throws IOException {
		// GIVEN
		UserEvent event = new UserEvent(1L, TEST_DATE, UUID.randomUUID(), "foo", null, null);

		// WHEN
		String json = mapper.writeValueAsString(event);

		// THEN
		// @formatter:off
		assertThat("JSON", json,
				is(equalTo("{\"userId\":" + event.getUserId()
						+ ",\"created\":\"" + TEST_DATE_STRING + "\""
						+ ",\"eventId\":\"" + event.getEventId() + "\""
						+ ",\"kind\":\"" + event.getKind() + "\""
						+ "}")));
		// @formatter:on
	}

}
