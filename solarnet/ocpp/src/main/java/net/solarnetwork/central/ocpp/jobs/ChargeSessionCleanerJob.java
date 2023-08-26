/* ==================================================================
 * ChargeSessionCleanerJob.java - 26/08/2023 4:01:21 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to delete charge sessions older than a certain period.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargeSessionCleanerJob extends JobSupport {

	/** The {@code expirePeriod} default value. */
	public static final Period DEFAULT_EXPIRE_PERIOD = Period.ofYears(1);

	private final Clock clock;
	private final CentralChargeSessionDao chargeSessionDao;
	private Period expirePeriod = DEFAULT_EXPIRE_PERIOD;

	private int deleteCount;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param chargeSessionDao
	 *        the charge session DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ChargeSessionCleanerJob(Clock clock, CentralChargeSessionDao chargeSessionDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.chargeSessionDao = requireNonNullArgument(chargeSessionDao, "chargeSessionDao");
		setGroupId("OCPP");
		setId("ChargeSessionCleaner");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		final Instant expireDate = clock.instant().atZone(ZoneOffset.UTC).minus(expirePeriod)
				.toInstant();
		log.info("Deleting posted OCPP charge sessions older than {} @ {}", expirePeriod, expireDate);
		final long start = System.currentTimeMillis();
		final int result = chargeSessionDao.deletePostedChargeSessions(expireDate);
		final long duration = System.currentTimeMillis() - start;
		this.deleteCount = result;
		log.info("Deleted {} expired OCPP charge sessions in {}ms", result, duration);
	}

	/**
	 * Get the last execution delete count.
	 * 
	 * @return the delete count
	 */
	public int getDeleteCount() {
		return deleteCount;
	}

	/**
	 * Get the expire period.
	 * 
	 * @return the expire period, never {@literal null}
	 */
	public Period getExpirePeriod() {
		return expirePeriod;
	}

	/**
	 * SEt the expire period.
	 * 
	 * @param expirePeriod
	 *        the period to set; if {@literal null} then
	 *        {@link #DEFAULT_EXPIRE_PERIOD} will be used instead
	 */
	public void setExpirePeriod(Period expirePeriod) {
		this.expirePeriod = (expirePeriod != null ? expirePeriod : DEFAULT_EXPIRE_PERIOD);
	}

}
