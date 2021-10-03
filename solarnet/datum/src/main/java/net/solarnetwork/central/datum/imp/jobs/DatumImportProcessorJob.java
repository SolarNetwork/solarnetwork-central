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

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to claim datum import tasks for processing and submit them for execution.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportProcessorJob extends JobSupport {

	private final DatumImportJobBiz importJobBiz;
	private int maximumClaimCount = 1000;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param importJobBiz
	 *        the service to use
	 */
	public DatumImportProcessorJob(EventAdmin eventAdmin, DatumImportJobBiz importJobBiz) {
		super(eventAdmin);
		this.importJobBiz = importJobBiz;
		setJobGroup("DatumImport");
		setMaximumWaitMs(5400000L);
		setMaximumClaimCount(1000);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		for ( int i = 0; i < maximumClaimCount; i++ ) {
			DatumImportJobInfo info = importJobBiz.claimQueuedJob();
			if ( info == null ) {
				// nothing left to claim
				break;
			}
			try {
				DatumImportStatus status = importJobBiz.performImport(info.getId());
				log.info("Submitted datum import task {}", status);
			} catch ( RuntimeException e ) {
				log.error("Error submitting datum import task {}", info, e);
				info.setMessage(e.getMessage());
				info.setJobSuccess(Boolean.FALSE);
				info.setImportState(DatumImportState.Completed);
				importJobBiz.saveJobInfo(info);
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
