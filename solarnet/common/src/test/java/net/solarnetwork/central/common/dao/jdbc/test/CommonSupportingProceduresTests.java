/* ==================================================================
 * CommonSupportingProceduresTests.java - 5/02/2019 12:42:01 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidUtils;

/**
 * Test cases for common supporting database functions.
 * 
 * @author matt
 * @version 2.2
 */
public class CommonSupportingProceduresTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Test
	public void formatRfc1123DateSmallDay() {
		Instant date = LocalDateTime.of(2019, 2, 4, 22, 30).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateBigDay() {
		Instant date = LocalDateTime.of(2019, 2, 11, 22, 30).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 11 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateSmallHourAndMinutesAndSeconds() {
		Instant date = LocalDateTime.of(2019, 2, 4, 2, 3, 4).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 02:03:04 GMT"));
	}

	@Test
	public void formatRfc1123DateBigHourAndMinutesAndSeconds() {
		Instant date = LocalDateTime.of(2019, 2, 4, 22, 33, 44).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:33:44 GMT"));
	}

	@Test
	public void uuidV7_toTimestamp() {
		// GIVEN
		Instant t = LocalDateTime.of(2022, 8, 3, 17, 25, 0, 123456789).toInstant(ZoneOffset.UTC);
		Clock fixed = Clock.fixed(t, ZoneOffset.UTC);
		TimeBasedV7UuidGenerator generator = new TimeBasedV7UuidGenerator(new SecureRandom(), fixed,
				UuidUtils.V7_MICRO_COUNT_PRECISION);
		UUID uuid = generator.generate();

		// WHEN
		Timestamp ts = jdbcTemplate.queryForObject("SELECT solarcommon.uuid_to_timestamp_v7(?)",
				Timestamp.class, uuid);

		// THEN
		assertThat("Timestamp extracted with microsecond precision", ts,
				is(equalTo(Timestamp.from(t.truncatedTo(ChronoUnit.MICROS)))));
	}

	@Test
	public void uuidV7_toTimestamp_zeroMicros() {
		// GIVEN
		Instant t = LocalDateTime.of(2022, 8, 3, 17, 25, 0, 123).toInstant(ZoneOffset.UTC);
		Clock fixed = Clock.fixed(t, ZoneOffset.UTC);
		TimeBasedV7UuidGenerator generator = new TimeBasedV7UuidGenerator(new SecureRandom(), fixed,
				UuidUtils.V7_MICRO_COUNT_PRECISION);
		UUID uuid = generator.generate();

		// WHEN
		Timestamp ts = jdbcTemplate.queryForObject("SELECT solarcommon.uuid_to_timestamp_v7(?)",
				Timestamp.class, uuid);

		// THEN
		assertThat("Timestamp extracted with microsecond precision", ts,
				is(equalTo(Timestamp.from(t.truncatedTo(ChronoUnit.MICROS)))));
	}

	@Test
	public void uuidV7_boundaryFromTimestamp() {

		// GIVEN
		Instant t = LocalDateTime.of(2022, 8, 3, 17, 25, 0, 123456789).toInstant(ZoneOffset.UTC);
		// WHEN
		UUID result = jdbcTemplate.queryForObject("SELECT solarcommon.timestamp_to_uuid_v7_boundary(?)",
				UUID.class, Timestamp.from(t));

		// THEN
		assertThat("UUID returned as boundary", result,
				is(equalTo(UuidUtils.createUuidV7Boundary(t.truncatedTo(ChronoUnit.MICROS)))));
	}


	@ParameterizedTest
	@CsvSource({ "1290,36", "255,16", "36,36", "1,36", "9223372036854775807,36" })
	public void toBaseX(long n, int radix) {
		// WHEN
		String result = jdbcTemplate.queryForObject("SELECT solarcommon.to_baseX(?, ?)", String.class,
				n, radix);

		// THEN
		// @formatter:off
		then(result)
			.as("Upper case base %d encoding of %d", radix, n)
			.isEqualTo(Long.toString(n, radix).toUpperCase(Locale.ENGLISH))
			;
		// @formatter:on
	}

	@Test
	public void toBaseX_defaultRadix() {
		// WHEN
		String result = jdbcTemplate.queryForObject("SELECT solarcommon.to_baseX(?)", String.class,
				255L);

		// THEN
		// @formatter:off
		then(result)
			.as("Base 16 used by default")
			.isEqualTo("FF")
			;
		// @formatter:on
	}

	@Test
	public void toBaseX_zero() {
		// WHEN
		String result = jdbcTemplate.queryForObject("SELECT solarcommon.to_baseX(?, ?)", String.class,
				0L, 36);

		// THEN
		// @formatter:off
		then(result)
			.as("Zero encoded")
			.isEqualTo("0")
			;
		// @formatter:on
	}

	@Test
	public void toBaseX_negative() {
		// WHEN
		String result = jdbcTemplate.queryForObject("SELECT solarcommon.to_baseX(?, ?)", String.class,
				-1L, 36);

		// THEN
		// @formatter:off
		then(result)
			.as("Negative numbers not supported")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void toBaseX_roundTrip() {
		// WHEN
		Long result = jdbcTemplate.queryForObject(
				"SELECT solarcommon.baseX_integer(solarcommon.to_baseX(?, ?), ?)", Long.class, 1290L,
				36, 36);

		// THEN
		// @formatter:off
		then(result)
			.as("Decoding the encoded value returns the original value")
			.isEqualTo(1290L)
			;
		// @formatter:on
	}

}
