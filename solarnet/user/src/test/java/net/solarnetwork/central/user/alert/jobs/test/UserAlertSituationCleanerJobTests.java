/* ==================================================================
 * UserAlertSituationCleanerJobTests.java - 8/03/2025 7:54:19 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.alert.jobs.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.user.alert.jobs.UserAlertSituationCleanerJob;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;

/**
 * Test cases for the {@link UserAlertSituationCleanerJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UserAlertSituationCleanerJobTests {

	@Mock
	private UserAlertSituationDao alertSituationDao;

	@Captor
	private ArgumentCaptor<Instant> instantCaptor;

	private MutableClock clock;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now(), ZoneOffset.UTC);
	}

	@Test
	public void executeJob() {
		// GIVEN
		var job = new UserAlertSituationCleanerJob(clock, alertSituationDao);
		job.setDaysOlder(CommonTestUtils.randomInt());

		final long updateCount = CommonTestUtils.randomLong();
		given(alertSituationDao.purgeResolvedSituations(any())).willReturn(updateCount);

		// WHEN
		job.run();

		// THEN
		then(alertSituationDao).should().purgeResolvedSituations(instantCaptor.capture());

		// @formatter:off
		and.then(instantCaptor.getValue())
			.as("Resolved purge instant is day offset from minute-truncated clock time")
			.isEqualTo(clock.instant().truncatedTo(ChronoUnit.MINUTES).minus(job.getDaysOlder(), ChronoUnit.DAYS))
			;
		// @formatter:on
	}

}
