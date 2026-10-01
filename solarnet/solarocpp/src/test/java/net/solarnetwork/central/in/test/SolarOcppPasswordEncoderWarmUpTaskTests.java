/* ==================================================================
 * SolarOcppPasswordEncoderWarmUpTaskTests.java - 1/10/2026 10:31:08 am
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

package net.solarnetwork.central.in.test;

import static net.solarnetwork.central.biz.AppWarmUpTask.IDENT;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.in.SolarOcppPasswordEncoderWarmUpTask;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for the {@link SolarOcppPasswordEncoderWarmUpTask} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SolarOcppPasswordEncoderWarmUpTaskTests {

	private static final String ENCODED = "$2a$12$encoded";

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private Clock clock;

	private SolarOcppPasswordEncoderWarmUpTask task;

	@BeforeEach
	public void setup() {
		task = new SolarOcppPasswordEncoderWarmUpTask(Clock.fixed(Instant.now(), ZoneOffset.UTC),
				passwordEncoder);
	}

	@Test
	public void warmUp() throws Exception {
		// GIVEN
		task.setIterations(3);

		given(passwordEncoder.encode(IDENT)).willReturn(ENCODED);
		given(passwordEncoder.matches(IDENT, ENCODED)).willReturn(true);

		// WHEN
		task.warmUp();

		// THEN
		// @formatter:off
		then(passwordEncoder).should().encode(IDENT);
		then(passwordEncoder).should(times(3)).matches(IDENT, ENCODED);
		// @formatter:on
	}

	@Test
	public void warmUp_maxDurationExceeded() throws Exception {
		// GIVEN
		task = new SolarOcppPasswordEncoderWarmUpTask(clock, passwordEncoder);
		task.setIterations(3);
		task.setMaxDuration(Duration.ofSeconds(10));

		final Instant start = Instant.now();
		// @formatter:off
		given(clock.instant()).willReturn(
				start,                   // warm-up start
				start.plusSeconds(1),    // encode done
				start.plusSeconds(2),    // iteration 1 start
				start.plusSeconds(8),    // iteration 1 done
				start.plusSeconds(10));  // iteration 2 start: deadline reached
		// @formatter:on

		given(passwordEncoder.encode(IDENT)).willReturn(ENCODED);
		given(passwordEncoder.matches(IDENT, ENCODED)).willReturn(true);

		// WHEN
		task.warmUp();

		// THEN
		// @formatter:off
		then(passwordEncoder).should().encode(IDENT);
		then(passwordEncoder).should(times(1)).matches(IDENT, ENCODED);
		// @formatter:on
	}

	@Test
	public void warmUp_noMatch() throws Exception {
		// GIVEN
		task.setIterations(3);

		given(passwordEncoder.encode(IDENT)).willReturn(ENCODED);
		given(passwordEncoder.matches(IDENT, ENCODED)).willReturn(false);

		// WHEN
		task.warmUp();

		// THEN
		// @formatter:off
		then(passwordEncoder).should(times(1)).matches(IDENT, ENCODED);
		// @formatter:on
	}

	@Test
	public void warmUp_encodeException() throws Exception {
		// GIVEN
		given(passwordEncoder.encode(IDENT)).willThrow(new RuntimeException("Boom"));

		// WHEN
		thenNoException().isThrownBy(task::warmUp);

		// THEN
		// @formatter:off
		then(passwordEncoder).should(times(0)).matches(any(), any());
		// @formatter:on
	}

}
