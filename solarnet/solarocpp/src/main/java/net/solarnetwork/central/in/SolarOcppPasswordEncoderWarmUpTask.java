/* ==================================================================
 * SolarOcppPasswordEncoderWarmUpTask.java - 1/10/2026 10:12:41 am
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

package net.solarnetwork.central.in;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.biz.AppWarmUpTask;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Component to "warm up" the password encoder used to authenticate charger
 * connections.
 *
 * <p>
 * Every OCPP WebSocket handshake verifies the charger credentials with the
 * password encoder, which is intentionally CPU-expensive. Until the JIT has
 * compiled the encoder's hashing loops they run several times slower. This
 * task runs the encoder before the web server accepts connections, so the
 * hashing code is compiled before chargers reconnect. The time each
 * verification takes is logged, which shows the per-handshake cost on the
 * deployed hardware.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@Component
@Profile(AppWarmUpTask.WARMUP)
public class SolarOcppPasswordEncoderWarmUpTask implements AppWarmUpTask {

	/** The {@code iterations} property default value. */
	public static final int DEFAULT_ITERATIONS = 2;

	/** The {@code maxDuration} property default value. */
	public static final Duration DEFAULT_MAX_DURATION = Duration.ofSeconds(60);

	private static final Logger log = LoggerFactory.getLogger(SolarOcppPasswordEncoderWarmUpTask.class);

	private final Clock clock;
	private final PasswordEncoder passwordEncoder;
	private int iterations = DEFAULT_ITERATIONS;
	private Duration maxDuration = DEFAULT_MAX_DURATION;

	/**
	 * Constructor.
	 *
	 * @param passwordEncoder
	 *        the password encoder to warm up
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@Autowired
	public SolarOcppPasswordEncoderWarmUpTask(PasswordEncoder passwordEncoder) {
		this(Clock.systemUTC(), passwordEncoder);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param passwordEncoder
	 *        the password encoder to warm up
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SolarOcppPasswordEncoderWarmUpTask(Clock clock, PasswordEncoder passwordEncoder) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.passwordEncoder = requireNonNullArgument(passwordEncoder, "passwordEncoder");
	}

	@Override
	public void warmUp() throws Exception {
		log.info("Performing password encoder warm-up tasks...");

		try {
			final Instant start = clock.instant();
			final Instant deadline = start.plus(maxDuration);

			log.debug("Encoding password...");
			final String encoded = passwordEncoder.encode(IDENT);
			log.info("Password encoder warm-up encode took {}ms",
					Duration.between(start, clock.instant()).toMillis());

			for ( int i = 1; i <= iterations; i++ ) {
				final Instant iterationStart = clock.instant();
				if ( !iterationStart.isBefore(deadline) ) {
					log.warn(
							"Password encoder warm-up stopped after {} of {} iterations: exceeded maximum duration {}s",
							i - 1, iterations, maxDuration.toSeconds());
					break;
				}
				final boolean matched = passwordEncoder.matches(IDENT, encoded);
				log.info("Password encoder warm-up iteration {} took {}ms", i,
						Duration.between(iterationStart, clock.instant()).toMillis());
				if ( !matched ) {
					log.warn("Password encoder warm-up password did not match encoded value.");
					break;
				}
			}
		} catch ( Exception e ) {
			log.error("App warm-up tasks threw exception: {}", e.getMessage(), e);
		}

		log.info("Password encoder warm-up tasks complete.");
	}

	/**
	 * Get the number of times to verify a password.
	 *
	 * @return the number of iterations; defaults to
	 *         {@link #DEFAULT_ITERATIONS}
	 */
	public final int getIterations() {
		return iterations;
	}

	/**
	 * Set the number of times to verify a password.
	 *
	 * @param iterations
	 *        the number of iterations to set
	 */
	public final void setIterations(int iterations) {
		this.iterations = iterations;
	}

	/**
	 * Get the maximum amount of time to spend warming up.
	 *
	 * <p>
	 * No new iteration starts once this much time has passed since the warm-up
	 * began, so startup is not delayed indefinitely on a CPU-starved host.
	 * </p>
	 *
	 * @return the maximum duration; defaults to {@link #DEFAULT_MAX_DURATION}
	 */
	public final Duration getMaxDuration() {
		return maxDuration;
	}

	/**
	 * Set the maximum amount of time to spend warming up.
	 *
	 * @param maxDuration
	 *        the maximum duration to set
	 * @throws IllegalArgumentException
	 *         if {@code maxDuration} is {@code null}
	 */
	public final void setMaxDuration(Duration maxDuration) {
		this.maxDuration = requireNonNullArgument(maxDuration, "maxDuration");
	}

}
