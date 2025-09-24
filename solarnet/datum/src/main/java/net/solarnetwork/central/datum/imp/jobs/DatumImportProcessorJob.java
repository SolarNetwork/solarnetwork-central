/* ==================================================================
 * DatumImportProcessorJob.java - 13/11/2018 4:19:27 PM
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

package net.solarnetwork.central.datum.imp.jobs;

import java.util.EnumSet;
import org.springframework.core.task.TaskRejectedException;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to claim datum import tasks for processing and submit them for execution.
 *
 * @author matt
 * @version 2.0
 */
public class DatumImportProcessorJob extends JobSupport {

	private final DatumImportJobBiz importJobBiz;

	/**
	 * Constructor.
	 *
	 * @param importJobBiz
	 *        the service to use
	 */
	public DatumImportProcessorJob(DatumImportJobBiz importJobBiz) {
		this.importJobBiz = ObjectUtils.requireNonNullArgument(importJobBiz, "importJobBiz");
		setGroupId("DatumImport");
		setMaximumWaitMs(5400000L);
	}

	@Override
	public void run() {
		final int max = getMaximumIterations();
		for ( int i = 0; i < max; i++ ) {
			DatumImportJobInfo info = importJobBiz.claimQueuedJob();
			if ( info == null ) {
				// nothing left to claim
				break;
			}
			try {
				DatumImportStatus status = importJobBiz.performImport(info.getId());
				log.info("Submitted datum import task {}", status);
			} catch ( TaskRejectedException e ) {
				log.debug("Import task rejected, setting back to Claimed state: {}", info);
				importJobBiz.updateJobState(info.getId(), DatumImportState.Queued,
						EnumSet.of(DatumImportState.Claimed));
				break;
			} catch ( RuntimeException e ) {
				log.error("Error submitting datum import task {}", info, e);
				info.setMessage(e.getMessage());
				info.setJobSuccess(Boolean.FALSE);
				info.setImportState(DatumImportState.Completed);
				importJobBiz.saveJobInfo(info);
			}
		}
	}

}
