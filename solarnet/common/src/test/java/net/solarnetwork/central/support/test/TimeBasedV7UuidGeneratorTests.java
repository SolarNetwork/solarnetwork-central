/* ==================================================================
 * TimeBasedV7UuidGeneratorTests.java - 3/08/2022 5:23:31 pm
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.TimeBasedV7UuidGenerator;
import net.solarnetwork.central.support.UuidUtils;

/**
 * Test cases for the {@link TimeBasedV7UuidGenerator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TimeBasedV7UuidGeneratorTests {

	private static final Logger log = LoggerFactory.getLogger(TimeBasedV7UuidGeneratorTests.class);

	private static Clock fixedClock() {
		Instant t = LocalDateTime.of(2022, 8, 3, 17, 25, 0, 123456789).toInstant(ZoneOffset.UTC);
		return Clock.fixed(t, ZoneOffset.UTC);
	}

	@Test
	public void create() {
		// GIVEN
		Clock fixed = fixedClock();
		TimeBasedV7UuidGenerator generator = new TimeBasedV7UuidGenerator(new SecureRandom(), fixed);

		// WHEN
		UUID uuid = generator.generate();
		log.info("Generated UUID: {}", uuid);

		// THEN
		assertThat("UUID generated", uuid, is(notNullValue()));
		assertThat("UUID version", uuid.version(), is(equalTo(7)));
		assertThat("UUID variant", uuid.variant(), is(equalTo(2)));
	}

	@Test
	public void timeOrder() {
		// GIVEN
		TimeBasedV7UuidGenerator generator = TimeBasedV7UuidGenerator.INSTANCE;

		// WHEN
		final SecureRandom r = new SecureRandom();
		final int count = 20;
		List<UUID> uuids = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			UUID uuid = generator.generate();
			uuids.add(uuid);
			try {
				Thread.sleep(Math.abs(r.nextLong()) % 100L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		log.debug("Generated {} v7 UUIDs: [{}]", uuids.size(),
				uuids.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));

		// THEN
		long prevTimestamp = 0;
		for ( UUID uuid : uuids ) {
			Instant curr = UuidUtils.extractTimestamp(uuid);
			assertThat("UUID time is never decreasing", curr.toEpochMilli(),
					is(greaterThanOrEqualTo(prevTimestamp)));
			prevTimestamp = curr.toEpochMilli();
		}
	}

}
