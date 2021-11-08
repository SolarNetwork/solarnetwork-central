/* ==================================================================
 * DatumImportProcessorJobTests.java - 13/11/2018 4:50:39 PM
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

import static org.easymock.EasyMock.expect;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.jobs.DatumImportProcessorJob;

/**
 * Test cases for the {@link DatumImportProcessorJob} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumImportProcessorJobTests {

	private static final String JOB_ID = "test.job";
	private static final Long TEST_USER_ID = 123L;

	private DatumImportJobBiz importJobBiz;

	private DatumImportProcessorJob job;

	@Before
	public void setup() {
		importJobBiz = EasyMock.createMock(DatumImportJobBiz.class);

		job = new DatumImportProcessorJob(importJobBiz);
		job.setId(JOB_ID);
		job.setMaximumClaimCount(2);
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
		DatumImportJobInfo info1 = new DatumImportJobInfo();
		info1.setId(new UserUuidPK(TEST_USER_ID, UUID.randomUUID()));
		expect(importJobBiz.claimQueuedJob()).andReturn(info1);
		DatumImportStatus status1 = EasyMock.createNiceMock(DatumImportStatus.class);
		expect(importJobBiz.performImport(info1.getId())).andReturn(status1);

		DatumImportJobInfo info2 = new DatumImportJobInfo();
		expect(importJobBiz.claimQueuedJob()).andReturn(info2);
		DatumImportStatus status2 = EasyMock.createNiceMock(DatumImportStatus.class);
		expect(importJobBiz.performImport(info2.getId())).andReturn(status2);

		// when
		replayAll();
		job.run();

		// then
	}

}
