/* ==================================================================
 * DateTimeUtilsTests.java - 15/05/2026 11:36:23 am
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

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.Period;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;
import net.solarnetwork.central.support.DateTimeUtils;

/**
 * Test cases for the {@link DateTimeUtils} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DateTimeUtilsTests {

	@Test
	public void parseInstant_null() {
		then(DateTimeUtils.parseInstant(null)).isNull();
	}

	@Test
	public void parseInstant_empty() {
		then(DateTimeUtils.parseInstant("")).isNull();
	}

	@Test
	public void parseInstant_blank() {
		then(DateTimeUtils.parseInstant(" ")).isNull();
	}

	@Test
	public void parseInstant_notValid() {
		then(DateTimeUtils.parseInstant("12/1/2026 12:25pm GMT")).isNull();
	}

	@Test
	public void parseInstant_iso_hm() {
		final var input = "2026-05-15T11:38Z";
		then(DateTimeUtils.parseInstant(input)).isNull();
	}

	@Test
	public void parseInstant_iso_hms() {
		final var input = "2026-05-15T11:38:00Z";
		then(DateTimeUtils.parseInstant(input)).isEqualTo(Instant.parse(input));
	}

	@Test
	public void parseInstant_iso_hmsS() {
		final var input = "2026-05-15T11:38:01.123456789Z";
		then(DateTimeUtils.parseInstant(input)).isEqualTo(Instant.parse(input));
	}

	@Test
	public void parseInstant_sn_hm() {
		final var input = "2026-05-15 11:38Z";
		then(DateTimeUtils.parseInstant(input))
				.isEqualTo(Instant.parse(input.replace(' ', 'T').replace("Z", ":00Z")));
	}

	@Test
	public void parseInstant_sn_hms() {
		final var input = "2026-05-15 11:38:00Z";
		then(DateTimeUtils.parseInstant(input)).isEqualTo(Instant.parse(input.replace(' ', 'T')));
	}

	@Test
	public void parseInstant_sn_hmsS() {
		final var input = "2026-05-15 11:38:01.123456789Z";
		then(DateTimeUtils.parseInstant(input)).isEqualTo(Instant.parse(input.replace(' ', 'T')));
	}

	@Test
	public void intervalMap_null() {
		then(DateTimeUtils.intervalMap(null)).isNull();
	}

	@Test
	public void intervalMap_empty() {
		then(DateTimeUtils.intervalMap(Map.of())).isNull();
	}

	@Test
	public void intervalMap_invalid() {
		then(DateTimeUtils.intervalMap(Map.of("foo", "bar"))).isNull();
	}

	@Test
	public void intervalMap_range() {
		final var start = "2020-05-15T11:38:00Z";
		final var end = "2026-05-15T11:38:00Z";
		then(DateTimeUtils.intervalMap(Map.of("foo", "%s/%s".formatted(start, end))))
				.containsOnly(Map.entry("foo", Interval.of(Instant.parse(start), Instant.parse(end))));
	}

	@Test
	public void intervalMap_invalidRemoved() {
		final var start = "2020-05-15T11:38:00Z";
		final var end = "2026-05-15T11:38:00Z";
		then(DateTimeUtils.intervalMap(Map.of("bad", "bunny", "foo", "%s/%s".formatted(start, end))))
				.containsOnly(Map.entry("foo", Interval.of(Instant.parse(start), Instant.parse(end))));
	}

	@Test
	public void intervalMap_noBeginning() {
		final var end = "2026-05-15T11:38:00Z";
		then(DateTimeUtils.intervalMap(Map.of("foo", "/%s".formatted(end))))
				.containsOnly(Map.entry("foo", Interval.endingAt(Instant.parse(end))));
	}

	@Test
	public void intervalMap_noEnd() {
		final var start = "2020-05-15T11:38:00Z";
		then(DateTimeUtils.intervalMap(Map.of("foo", "%s/".formatted(start))))
				.containsOnly(Map.entry("foo", Interval.startingAt(Instant.parse(start))));
	}

	@Test
	public void comparePeriods_equal() {
		// GIVEN
		final Period l = Period.of(1, 2, 3);
		final Period r = Period.of(1, 2, 3);

		// THEN
		then(DateTimeUtils.comparePeriods(l, r)).isEqualTo(0);
		then(DateTimeUtils.comparePeriods(r, l)).isEqualTo(0);
	}

	@Test
	public void comparePeriods_equal_normalized() {
		// GIVEN
		final Period l = Period.of(0, 12, 0);
		final Period r = Period.of(1, 0, 0);

		// THEN
		then(DateTimeUtils.comparePeriods(l, r)).isEqualTo(0);
		then(DateTimeUtils.comparePeriods(r, l)).isEqualTo(0);
	}

	@Test
	public void comparePeriods_less() {
		// GIVEN
		final Period l = Period.of(0, 1, 2);
		final Period r = Period.of(1, 2, 3);

		// THEN
		then(DateTimeUtils.comparePeriods(l, r)).isLessThan(0);
		then(DateTimeUtils.comparePeriods(r, l)).isGreaterThan(0);
	}

}
