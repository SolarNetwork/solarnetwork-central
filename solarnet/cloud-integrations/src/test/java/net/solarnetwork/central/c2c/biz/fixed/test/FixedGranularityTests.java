/* ==================================================================
 * FixedGranularityTests.java - 13 Sept 2026 6:39:50 pm
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

package net.solarnetwork.central.c2c.biz.fixed.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import java.time.Duration;
import java.time.Period;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.solarnetwork.central.c2c.biz.fixed.FixedGranularity;

/**
 * Test cases for the {@link FixedGranularity} class.
 *
 * @author matt
 * @version 1.0
 */
public class FixedGranularityTests {

	@ParameterizedTest
	@ValueSource(strings = { "PT1M", "PT5M", "PT10M", "PT15M", "PT20M", "PT30M", "PT1H" })
	public void parseIsoDurations(String iso) {
		// GIVEN
		final Duration dur = Duration.parse(iso);

		// WHEN
		final FixedGranularity result = FixedGranularity.fromValue(iso);

		// THEN
		// @formatter:off
		then(result)
			.as("ISO duration parsed")
			.isNotNull()
			.as("Value for expected duration")
			.returns(dur, from(FixedGranularity::getTickAmount))
			;

		then(FixedGranularity.fromValue(iso.toLowerCase(Locale.ROOT)))
			.as("Lower-case input parsed as well")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = { "P1D", "P1M", "P1Y" })
	public void parseIsoPeriods(String iso) {
		// GIVEN
		final Period period = Period.parse(iso);

		// WHEN
		final FixedGranularity result = FixedGranularity.fromValue(iso);

		// THEN
		// @formatter:off
		then(result)
			.as("ISO period parsed")
			.isNotNull()
			.as("Value for expected duration")
			.returns(period, from(FixedGranularity::getTickAmount))
			;

		then(FixedGranularity.fromValue(iso.toLowerCase(Locale.ROOT)))
			.as("Lower-case input parsed as well")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void parseNamesCaseInsensitive() {
		for ( FixedGranularity e : FixedGranularity.values() ) {
			// @formatter:off
			then(FixedGranularity.fromValue(e.name().toLowerCase()))
				.as("Case-insensitive name parsed")
				.isSameAs(e)
				;
			// @formatter:on
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "PT1S", "PT2M", "PT2H" })
	public void unsupportedDuration(String input) {
		thenThrownBy(() -> {
			FixedGranularity.fromValue(input);
		}, "Valid ISO duration [%s] is not supported", input)
				.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "P1M1D", "P2D", "P2M", "P2Y" })
	public void unsupportedPeriod(String input) {
		thenThrownBy(() -> {
			FixedGranularity.fromValue(input);
		}, "Valid ISO period [%s] is not supported", input).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "foo", "5min", "every day" })
	public void invalidInput(String input) {
		thenThrownBy(() -> {
			FixedGranularity.fromValue(input);
		}, "Invalid input [%s] is not supported", input).isInstanceOf(IllegalArgumentException.class);
	}

}
