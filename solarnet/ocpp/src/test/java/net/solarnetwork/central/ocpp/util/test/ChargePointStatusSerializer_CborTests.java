/* ==================================================================
 * ChargePointStatusSerializerTests.java - 26/01/2023 2:08:27 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.util.test;

import static net.solarnetwork.util.ByteUtils.objectArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.ocpp.util.ChargePointStatusSerializer;

/**
 * Test cases for the {@link ChargePointStatusSerializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointStatusSerializer_CborTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MICROSECONDS.toNanos(234567))
			.toInstant(ZoneOffset.UTC);

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = new ObjectMapper(new CBORFactory());
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(ChargePointStatus.class, ChargePointStatusSerializer.INSTANCE);
		m.registerModule(mod);
		return m;
	}

	@BeforeEach
	public void setup() {
		mapper = createObjectMapper();
	}

	@Test
	public void serialize_connected() throws IOException {
		// GIVEN
		ChargePointStatus status = new ChargePointStatus(1L, 2L,
				TEST_DATE.truncatedTo(ChronoUnit.MILLIS).minusSeconds(1), UUID.randomUUID().toString(),
				UUID.randomUUID().toString(), TEST_DATE.truncatedTo(ChronoUnit.MILLIS));

		// WHEN
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(status));

		// THEN
		assertThat("CBOR", cbor, is(arrayWithSize(196)));
	}

	@Test
	public void serialize_disconnected() throws IOException {
		// GIVEN
		ChargePointStatus status = new ChargePointStatus(1L, 2L,
				TEST_DATE.truncatedTo(ChronoUnit.MILLIS).minusSeconds(1), null, null,
				TEST_DATE.truncatedTo(ChronoUnit.MILLIS));

		// WHEN
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(status));

		// THEN
		assertThat("CBOR", cbor, is(arrayWithSize(98)));
	}

}
