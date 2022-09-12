/* ==================================================================
 * UuidUtilsTests.java - 12/09/2022 12:44:28 pm
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.UuidUtils;

/**
 * Test cases for the {@link UuidUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UuidUtilsTests {

	private Instant testInstant() {
		return LocalDateTime.of(2022, 8, 3, 17, 25, 0, 123456789).toInstant(ZoneOffset.UTC);
	}

	@Test
	public void extractTimestamp_v7() {
		// GIVEN
		UUID uuid = UUID.fromString("018264bd-3e5b-7723-b2fe-4435fd443b4e");

		// WHEN
		Instant ts = UuidUtils.extractTimestamp(uuid);

		// THEN
		assertThat("Timestamp is expected", ts,
				is(equalTo(testInstant().truncatedTo(ChronoUnit.MILLIS))));

	}

	@Test
	public void extractTimestamp_v7_withMicros() {
		// GIVEN
		UUID uuid = UUID.fromString("018264bd-3e5b-7720-b7c6-6d0bd3434a5e");

		// WHEN
		Instant ts = UuidUtils.extractTimestamp(uuid, true);

		// THEN
		assertThat("Timestamp is expected", ts,
				is(equalTo(testInstant().truncatedTo(ChronoUnit.MICROS))));

	}

}
