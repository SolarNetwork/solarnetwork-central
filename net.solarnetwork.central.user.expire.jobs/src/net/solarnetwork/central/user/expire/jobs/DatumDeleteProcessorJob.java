/* ==================================================================
 * DatumDeleteProcessorJob.java - 13/11/2018 4:19:27 PM
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

package net.solarnetwork.central.user.expire.jobs;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteJobBiz;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobStatus;

/**
 * Job to claim datum delete jobs for processing and submit them for execution.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDeleteProcessorJob extends JobSupport {

	private final UserDatumDeleteJobBiz datumDeleteJobBiz;
	private final UserDatumDeleteJobInfoDao jobInfoDao;
	private int maximumClaimCount = 1;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param datumDeleteJobBiz
	 *        the service to use
	 * @param jobInfoDao
	 *        the DAO to use
	 */
	public DatumDeleteProcessorJob(EventAdmin eventAdmin, UserDatumDeleteJobBiz datumDeleteJobBiz,
			UserDatumDeleteJobInfoDao jobInfoDao) {
		super(eventAdmin);
		this.datumDeleteJobBiz = datumDeleteJobBiz;
		this.jobInfoDao = jobInfoDao;
		setJobGroup("DatumImport");
		setMaximumWaitMs(5400000L);
		setMaximumClaimCount(1000);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		for ( int i = 0; i < maximumClaimCount; i++ ) {
			DatumDeleteJobInfo info = jobInfoDao.claimQueuedJob();
			if ( info == null ) {
				// nothing left to claim
				break;
			}
			try {
				DatumDeleteJobStatus status = datumDeleteJobBiz.performDatumDelete(info.getId());
				log.info("Submitted datum import task {}", status);
			} catch ( RuntimeException e ) {
				log.error("Error submitting datum import task {}", info, e);
				info.setMessage(e.getMessage());
				info.setJobSuccess(Boolean.FALSE);
				info.setJobState(DatumDeleteJobState.Completed);
				jobInfoDao.store(info);
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
