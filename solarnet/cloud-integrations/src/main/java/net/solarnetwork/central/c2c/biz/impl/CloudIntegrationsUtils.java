/* ==================================================================
 * CloudIntegrationsUtils.java - 26/10/2024 6:31:28 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl;

import static java.time.ZoneOffset.UTC;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Helper methods for cloud integrations.
 *
 * @author matt
 * @version 1.0
 */
public final class CloudIntegrationsUtils {

	private CloudIntegrationsUtils() {
		// not available
	}

	/** Constant for 1e-3. */
	public static final BigDecimal MILLIS = new BigDecimal(BigInteger.ONE, 3);

	/** Constant for number of seconds per hour. */
	public static final BigDecimal SECS_PER_HOUR = new BigDecimal(3600);

	/**
	 * Truncate a date based on a duration.
	 *
	 * @param date
	 *        the date to truncate
	 * @param period
	 *        the duration to truncate to
	 * @return the truncated date
	 */
	public static Instant truncateDate(Instant date, Duration period) {
		return Clock.tick(Clock.fixed(date, UTC), period).instant();
	}

}
