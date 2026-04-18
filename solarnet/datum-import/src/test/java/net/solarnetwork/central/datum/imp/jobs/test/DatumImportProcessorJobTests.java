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

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.jobs.DatumImportProcessorJob;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Test cases for the {@link DatumImportProcessorJob} class.
 *
 * @author matt
 * @version 2.1
 */
@ExtendWith(MockitoExtension.class)
public class DatumImportProcessorJobTests {

	@Mock
	private DatumImportJobBiz importJobBiz;

	private static final Long TEST_USER_ID = 123L;

	private DatumImportProcessorJob job;

	@BeforeEach
	public void setup() {
		job = new DatumImportProcessorJob(importJobBiz);
		job.setMaximumIterations(2);
	}

	@Test
	public void executeJob() {
		// GIVEN
		DatumImportJobInfo info1 = new DatumImportJobInfo(new UserUuidPK(TEST_USER_ID, randomUUID()),
				now());
		DatumImportStatus status1 = Mockito.mock(DatumImportStatus.class);

		DatumImportJobInfo info2 = new DatumImportJobInfo(new UserUuidPK(TEST_USER_ID, randomUUID()),
				now());
		DatumImportStatus status2 = Mockito.mock(DatumImportStatus.class);

		given(importJobBiz.claimQueuedJob()).willReturn(info1, info2);
		given(importJobBiz.performImport(info1.getId())).willReturn(status1);
		given(importJobBiz.performImport(info2.getId())).willReturn(status2);

		// WHEN
		job.run();

		// THEN
		then(importJobBiz).shouldHaveNoMoreInteractions();
	}

	@Test
	public void executeJob_rejected() {
		// GIVEN
		DatumImportJobInfo info1 = new DatumImportJobInfo(new UserUuidPK(TEST_USER_ID, randomUUID()),
				now());
		given(importJobBiz.claimQueuedJob()).willReturn(info1);

		TaskRejectedException tre = new TaskRejectedException("Rejected");
		given(importJobBiz.performImport(info1.getId())).willThrow(tre);

		// WHEN
		job.run();

		// THEN
		then(importJobBiz).should().updateJobState(info1.getId(), DatumImportState.Queued,
				EnumSet.of(DatumImportState.Claimed));
		then(importJobBiz).shouldHaveNoMoreInteractions();
	}

}
