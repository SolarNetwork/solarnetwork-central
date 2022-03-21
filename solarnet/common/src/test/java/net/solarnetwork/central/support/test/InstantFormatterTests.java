/* ==================================================================
 * InstantFormatterTests.java - 22/03/2022 10:18:53 AM
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
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.InstantFormatter;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link InstantFormatter} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InstantFormatterTests {

	private InstantFormatter defaultInstance() {
		return new InstantFormatter(DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC,
				DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC);
	}

	@Test
	public void parse_full_millis() throws ParseException {
		// GIVEN
		String s = "2021-08-11 11:47:00.123";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime
				.of(2021, 8, 11, 11, 47, 0, (int) TimeUnit.MILLISECONDS.toNanos(123))
				.toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_full_millis_t() throws ParseException {
		// GIVEN
		String s = "2021-08-11T11:47:00.123";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime
				.of(2021, 8, 11, 11, 47, 0, (int) TimeUnit.MILLISECONDS.toNanos(123))
				.toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_full() throws ParseException {
		// GIVEN
		String s = "2021-08-11 11:47:01";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime.of(2021, 8, 11, 11, 47, 1, 0).toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_full_t() throws ParseException {
		// GIVEN
		String s = "2021-08-11T11:47:01";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime.of(2021, 8, 11, 11, 47, 1, 0).toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_minute() throws ParseException {
		// GIVEN
		String s = "2021-08-11 11:47";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime.of(2021, 8, 11, 11, 47, 0, 0).toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_minute_t() throws ParseException {
		// GIVEN
		String s = "2021-08-11T11:47";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime.of(2021, 8, 11, 11, 47, 0, 0).toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

	@Test
	public void parse_date() throws ParseException {
		// GIVEN
		String s = "2021-08-11";

		// WHEN
		Instant result = defaultInstance().parse(s, Locale.getDefault());

		// THEN
		Instant expected = LocalDateTime.of(2021, 8, 11, 0, 0, 0, 0).toInstant(ZoneOffset.UTC);
		assertThat("Instant parsed", result, is(equalTo(expected)));
	}

}
