/* ==================================================================
 * DatumExportProcessor.java - 18/04/2018 6:23:15 AM
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

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
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
 * @version 1.0
 */
public class DatumExportProcessor extends JobSupport {

	private final DatumExportTaskInfoDao taskDao;
	private final DatumExportBiz datumExportBiz;
	private int maximumClaimCount = 1000;

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param taskDao
	 *        the DAO to use
	 * @param datumExportBiz
	 *        the export service to use
	 */
	public DatumExportProcessor(EventAdmin eventAdmin, DatumExportTaskInfoDao taskDao,
			DatumExportBiz datumExportBiz) {
		super(eventAdmin);
		this.taskDao = taskDao;
		this.datumExportBiz = datumExportBiz;
		setJobGroup("DatumExport");
		setMaximumWaitMs(1800000L);
		setMaximumClaimCount(1000);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		for ( int i = 0; i < maximumClaimCount; i++ ) {
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
		return true;
	}

	/**
	 * Set the maximum number of claims to acquire per execution of this job.
	 * 
	 * @param maximumClaimCount
	 *        the maximum count; defaults to {@literal 1000}
	 */
	public void setMaximumClaimCount(int maximumClaimCount) {
		this.maximumClaimCount = maximumClaimCount;
	}

}
