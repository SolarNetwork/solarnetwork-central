/* ==================================================================
 * DelegatingParserTests.java - 7/04/2022 8:58:50 AM
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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.format.datetime.standard.TemporalAccessorParser;
import net.solarnetwork.central.support.DelegatingParser;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link DelegatingParser} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingParserTests {

	private DelegatingParser<TemporalAccessor> localDateTimeWithFallback() {
		return new DelegatingParser<TemporalAccessor>(
				new TemporalAccessorParser(LocalDateTime.class,
						DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC),
				new TemporalAccessorParser(LocalDateTime.class,
						DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_ALT_UTC));
	}

	@Test
	public void localDateTime_primary_success() throws ParseException {
		// GIVEN
		DelegatingParser<TemporalAccessor> p = localDateTimeWithFallback();

		// THEN
		assertThat("Date parses with fallback", p.parse("2022-04-07T12:34", Locale.getDefault()),
				is(LocalDateTime.of(2022, 4, 7, 12, 34)));
	}

	@Test
	public void localDateTime_fallback_success() throws ParseException {
		// GIVEN
		DelegatingParser<TemporalAccessor> p = localDateTimeWithFallback();

		// THEN
		assertThat("Date parses with fallback", p.parse("2022-04-07 12:34", Locale.getDefault()),
				is(LocalDateTime.of(2022, 4, 7, 12, 34)));
	}

	@Test
	public void localDateTime_failure() throws ParseException {
		// GIVEN
		DelegatingParser<TemporalAccessor> p = localDateTimeWithFallback();

		// THEN
		assertThrows(DateTimeParseException.class, () -> {
			p.parse("2022-04-07 A", Locale.getDefault());
		});
	}

}
