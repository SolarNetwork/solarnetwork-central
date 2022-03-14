/* ==================================================================
 * UserExportTaskCleanerJobTests.java - 28/04/2018 3:42:58 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.jobs.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.jobs.UserExportTaskCleanerJob;

/**
 * Test cases for the [@link UserExportTaskCleanerJob} class.
 * 
 * @author matt
 * @version 2.0
 */
public class UserExportTaskCleanerJobTests {

	private static final String JOB_ID = "test.job";
	private static final int EXPIRE_MINS = 10;

	private UserDatumExportTaskInfoDao taskDao;

	private UserExportTaskCleanerJob job;

	@Before
	public void setup() {
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);

		job = new UserExportTaskCleanerJob(taskDao);
		job.setId(JOB_ID);
		job.setMinimumAgeMinutes(EXPIRE_MINS);
	}

	private void replayAll() {
		EasyMock.replay(taskDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(taskDao);
	}

	@Test
	public void executeJob() {
		// given
		Capture<Instant> dateCaptor = new Capture<>();
		expect(taskDao.purgeCompletedTasks(capture(dateCaptor))).andReturn(1L);

		// when
		replayAll();
		Instant now = Instant.now();
		job.run();

		// then
		assertThat("Date minutes OK",
				(int) ChronoUnit.MINUTES.between(dateCaptor.getValue(), now.plus(1, ChronoUnit.MINUTES)),
				equalTo(EXPIRE_MINS));
	}

}
