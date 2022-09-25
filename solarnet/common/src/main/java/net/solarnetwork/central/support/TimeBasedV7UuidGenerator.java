/* ==================================================================
 * TimeBasedUuidGenerator.java - 2/08/2022 5:32:23 pm
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import net.solarnetwork.central.biz.UuidGenerator;
import net.solarnetwork.central.biz.UuidTimestampDecoder;

/**
 * UUID generator using time-based v7 UUIDs.
 * 
 * <p>
 * See <a href=
 * "https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#name-uuid-version-7">the
 * IETF working draft</a> for details.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class TimeBasedV7UuidGenerator implements UuidGenerator, UuidTimestampDecoder {

	/**
	 * A default instance.
	 */
	public static final TimeBasedV7UuidGenerator INSTANCE;
	static {
		Clock c = Clock.tickMillis(ZoneOffset.UTC);
		TimeBasedV7UuidGenerator g;
		try {
			g = new TimeBasedV7UuidGenerator(SecureRandom.getInstanceStrong(), c);
		} catch ( NoSuchAlgorithmException e ) {
			g = new TimeBasedV7UuidGenerator(new SecureRandom(), c);
		}
		INSTANCE = g;
	}

	/**
	 * A default instance with microsecond time precision.
	 * 
	 * @since 1.1
	 */
	public static final TimeBasedV7UuidGenerator INSTANCE_MICROS;
	static {
		Clock c = Clock.tick(Clock.systemUTC(), Duration.of(1, ChronoUnit.MICROS));
		TimeBasedV7UuidGenerator g;
		try {
			g = new TimeBasedV7UuidGenerator(SecureRandom.getInstanceStrong(), c, true);
		} catch ( NoSuchAlgorithmException e ) {
			g = new TimeBasedV7UuidGenerator(new SecureRandom(), c, true);
		}
		INSTANCE_MICROS = g;
	}

	private final SecureRandom rand;
	private final Clock clock;
	private final boolean includeMicros;

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 * @param clock
	 *        the clock
	 */
	public TimeBasedV7UuidGenerator(SecureRandom rand, Clock clock) {
		this(rand, clock, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 * @param clock
	 *        the clock
	 * @param includeMicros
	 *        include microseconds in the UUID, for higher-resolution time-based
	 *        sorting
	 * @since 1.1
	 */
	public TimeBasedV7UuidGenerator(SecureRandom rand, Clock clock, boolean includeMicros) {
		super();
		this.rand = requireNonNullArgument(rand, "rand");
		this.clock = requireNonNullArgument(clock, "clock");
		this.includeMicros = includeMicros;
	}

	@Override
	public UUID generate() {
		final Instant now = clock.instant();

		// @formatter:off
		long lower = 0xB000000000000000L | (rand.nextLong() & 0x3FFFFFFFFFFFFFFFL);
		byte[] r = new byte[2];
		rand.nextBytes(r);
		long upper = (
				((now.toEpochMilli() & 0xFFFFFFFFFFFFL) << 16) // truncate epoch to 48 bits
				| 0x7000L // variant
				| ((r[0] & 0x0F) << 8) // rand_a 12 bits
				| (r[1] & 0xFF)
				);
		if ( includeMicros ) {
			// use 10 bits for microseconds; preserve last 2 random bits
			int micros = ((now.getNano() / 1_000) - ((now.getNano() / 1_000_000) * 1_000));
			upper = (upper & 0xFFFFFFFF_FFFFF003L) | (micros << 2);
		}
		// @formatter:on
		return new UUID(upper, lower);
	}

	@Override
	public Instant decodeTimestamp(UUID uuid) {
		return UuidUtils.extractTimestampV7(uuid, includeMicros);
	}

	@Override
	public UUID createTimestampBoundary(Instant ts) {
		return createBoundary(ts);
	}

	/**
	 * Generate a V7 UUID "boundary" marker.
	 * 
	 * <p>
	 * The returned value has no random data. It only includes the timestamp
	 * value.
	 * </p>
	 * 
	 * @param ts
	 *        the timestamp to encode
	 * @return the UUID
	 */
	public static UUID createBoundary(Instant ts) {
		long now = ts.toEpochMilli();

		// @formatter:off
		long lower = 0xB000000000000000L;
		long upper = (
				((now & 0xFFFFFFFFFFFFL) << 16) // truncate epoch to 48 bits
				| 0x7000L // variant
				);
		// @formatter:on
		return new UUID(upper, lower);
	}

}
