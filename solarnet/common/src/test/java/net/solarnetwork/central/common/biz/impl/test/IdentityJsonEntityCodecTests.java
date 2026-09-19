/* ==================================================================
 * IdentityJsonEntityCodecTests.java - 18/03/2026 6:09:02 pm
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

package net.solarnetwork.central.common.biz.impl.test;

import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.biz.impl.IdentityJsonEntityCodec;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.central.support.UserEventBasicSerializer;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test cases for the {@link IdentityJsonEntityCodec} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IdentityJsonEntityCodecTests {

	public static final JacksonModule EVENT_MODULE;
	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventBasicSerializer.INSTANCE);
		m.addDeserializer(UserEvent.class, UserEventBasicDeserializer.INSTANCE);
		EVENT_MODULE = m;
	}

	private static final JsonMapper JSON_MAPPER = JsonUtils.JSON_OBJECT_MAPPER.rebuild()
			.addModule(EVENT_MODULE).build();

	@Test
	public void codec() {
		// GIVEN
		final var service = new IdentityJsonEntityCodec<>(JSON_MAPPER, UserEvent.class);

		final UserEvent event = new UserEvent(randomLong(), randomUUID(),
				new String[] { randomString(), randomString() }, null, null);

		// WHEN
		String serialized = service.serialize(event);
		UserUuidPK id = service.entityId(event);
		UserEvent deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("userId", event.getUserId()),
				entry("eventId", event.getEventId().toString()),
				entry("tags", event.getTags())
			)
			;
		
		then(id)
			.as("Entity ID returned")
			.isSameAs(event.getId())
			;
		
		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to original event")
			.isEqualTo(event)
			;
		// @formatter:on
	}

}
