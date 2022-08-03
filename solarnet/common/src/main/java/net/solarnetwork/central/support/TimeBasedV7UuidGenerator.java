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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import net.solarnetwork.central.biz.UuidGenerator;
import net.solarnetwork.central.biz.UuidTimestampExtractor;

/**
 * UUID generator using time-based v1 UUIDs.
 * 
 * @author matt
 * @version 1.0
 */
public class TimeBasedV7UuidGenerator implements UuidGenerator, UuidTimestampExtractor {

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

	private final SecureRandom rand;
	private final long lower;
	private final Clock clock;

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 */
	public TimeBasedV7UuidGenerator(SecureRandom rand, Clock clock) {
		super();
		this.rand = requireNonNullArgument(rand, "rand");
		this.lower = 0xB000000000000000L | (rand.nextLong() & 0x3FFFFFFFFFFFFFFFL);
		this.clock = requireNonNullArgument(clock, "clock");
	}

	@Override
	public UUID generate() {
		long now = clock.millis();

		// @formatter:off
		byte[] r = new byte[2];
		rand.nextBytes(r);
		long upper = (
				((now & 0xFFFFFFFFFFFFL) << 16) // truncate epoch to 48 bits
				| 0x7000L // variant
				| ((r[0] << 8) & 0x0F) // rand_a 12 bits
				| (r[1] & 0xFF)
				);
		// @formatter:on
		return new UUID(upper, lower);
	}

	@Override
	public Instant extractTimestamp(UUID uuid) {
		return extractUuidTimestamp(uuid);
	}

	/**
	 * Extract the timestamp out of a UUID.
	 * 
	 * <p>
	 * Only version 7 and 1 are supported.
	 * </p>
	 * 
	 * @param uuid
	 *        the UUID to extract
	 * @return the timestamp, or {@literal null} if unable to extract a
	 *         timestamp
	 */
	public static Instant extractUuidTimestamp(UUID uuid) {
		if ( uuid.version() == 7 ) {
			return Instant.ofEpochMilli((uuid.getMostSignificantBits() >> 16) & 0xFFFFFFFFFFFFL);
		} else if ( uuid.version() == 1 ) {
			return Instant.ofEpochMilli(uuid.timestamp());
		}
		return null;
	}

}
