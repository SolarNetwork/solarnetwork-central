/* ==================================================================
 * DatumImportJobInfoCleanerJobTests.java - 13/11/2018 4:51:04 PM
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

package net.solarnetwork.central.datum.imp.jobs.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import java.time.Instant;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.jobs.DatumImportJobInfoCleanerJob;

/**
 * Test cases for the {@link DatumImportJobInfoCleanerJob} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumImportJobInfoCleanerJobTests {

	private static final String JOB_ID = "test.job";
	private static final int EXPIRE_MINS = 10;

	private DatumImportJobBiz importJobBiz;

	private DatumImportJobInfoCleanerJob job;

	@Before
	public void setup() {
		importJobBiz = EasyMock.createMock(DatumImportJobBiz.class);

		job = new DatumImportJobInfoCleanerJob(importJobBiz);
		job.setId(JOB_ID);
		job.setMinimumAgeMinutes(EXPIRE_MINS);
	}

	private void replayAll() {
		EasyMock.replay(importJobBiz);
	}

	@After
	public void teardown() {
		EasyMock.verify(importJobBiz);
	}

	@Test
	public void executeJob() {
		// given
		Capture<Instant> timeCaptor = new Capture<>();
		expect(importJobBiz.purgeOldJobs(capture(timeCaptor))).andReturn(1L);

		// when
		replayAll();
		job.run();

		// then
	}

}
