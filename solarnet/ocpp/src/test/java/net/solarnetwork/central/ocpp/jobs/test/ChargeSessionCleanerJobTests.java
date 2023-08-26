/* ==================================================================
 * ChargeSessionCleanerJobTests.java - 26/08/2023 4:09:03 pm
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

package net.solarnetwork.central.ocpp.jobs.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.jobs.ChargeSessionCleanerJob;

/**
 * Test cases for the {@link ChargeSessionCleanerJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class ChargeSessionCleanerJobTests {

	@Mock
	private CentralChargeSessionDao chargeSessionDao;

	private Clock clock;
	private ChargeSessionCleanerJob job;

	@BeforeEach
	public void setup() {
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
		job = new ChargeSessionCleanerJob(clock, chargeSessionDao);
	}

	@Test
	public void execute() {
		// GIVEN
		final Period period = Period.ofMonths(1);
		final ZonedDateTime expectedDate = clock.instant().atZone(ZoneOffset.UTC).minus(period);
		final int resultCount = new SecureRandom().nextInt();
		given(chargeSessionDao.deletePostedChargeSessions(expectedDate.toInstant()))
				.willReturn(resultCount);

		// WHEN
		job.setExpirePeriod(period);
		job.run();

		// THEN
		then(resultCount).as("Result count is from DAO result").isEqualTo(resultCount);
	}

}
