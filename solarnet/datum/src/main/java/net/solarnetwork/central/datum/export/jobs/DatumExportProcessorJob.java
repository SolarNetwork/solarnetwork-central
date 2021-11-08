/* ==================================================================
 * DatumExportProcessorJob.java - 18/04/2018 6:23:15 AM
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

package net.solarnetwork.central.datum.export.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.datum.export.biz.DatumExportBiz;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportStatus;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to claim datum export tasks for processing and submit them for execution.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumExportProcessorJob extends JobSupport {

	private final DatumExportTaskInfoDao taskDao;
	private final DatumExportBiz datumExportBiz;

	/**
	 * Construct with properties.
	 * 
	 * @param taskDao
	 *        the DAO to use
	 * @param datumExportBiz
	 *        the export service to use
	 */
	public DatumExportProcessorJob(DatumExportTaskInfoDao taskDao, DatumExportBiz datumExportBiz) {
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.datumExportBiz = requireNonNullArgument(datumExportBiz, "datumExportBiz");
		setGroupId("DatumExport");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		final int max = getMaximumIterations();
		for ( int i = 0; i < max; i++ ) {
			DatumExportTaskInfo task = taskDao.claimQueuedTask();
			if ( task == null ) {
				// nothing left to claim
				break;
			}
			try {
				DatumExportStatus status = datumExportBiz.performExport(task);
				log.info("Submitted datum export task {}", status);
			} catch ( RuntimeException e ) {
				log.error("Error submitting datum export task {}", task, e);
				task.setMessage(e.getMessage());
				task.setTaskSuccess(Boolean.FALSE);
				task.setStatus(DatumExportState.Completed);
				taskDao.store(task);
			}
		}
	}

}
