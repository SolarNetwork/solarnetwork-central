/* ==================================================================
 * SchedulerAdminController.java - 26/01/2018 7:29:03 AM
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

package net.solarnetwork.central.jobs.web.api;

import static net.solarnetwork.web.domain.Response.response;
import java.util.Collection;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.jobs.web.domain.JobFilter;
import net.solarnetwork.central.scheduler.JobInfo;
import net.solarnetwork.central.scheduler.SchedulerManager;
import net.solarnetwork.central.scheduler.SchedulerStatus;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;

/**
 * REST controller for job scheduler management.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1SchedulerController")
@RequestMapping(value = { "/api/v1/sec/scheduler" })
public class SchedulerAdminController extends WebServiceControllerSupport {

	private final SchedulerManager schedulerManager;

	/**
	 * Constructor.
	 * 
	 * @param schedulerManager
	 *        the manager to use
	 */
	@Autowired
	public SchedulerAdminController(SchedulerManager schedulerManager) {
		super();
		this.schedulerManager = schedulerManager;
	}

	/**
	 * Get the scheduler's current status.
	 * 
	 * @return the status
	 */
	@ResponseBody
	@RequestMapping(value = "/status", method = RequestMethod.GET)
	public Response<SchedulerStatus> currentStatus() {
		return response(schedulerManager.currentStatus());
	}

	/**
	 * Pause a specific job.
	 * 
	 * @param groupId
	 *        the group ID of the job to pause
	 * @param id
	 *        the ID of the job to pause
	 * @return the response
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/pause", method = RequestMethod.POST)
	public Response<Void> pauseJob(
			@RequestParam(value = "groupId", required = false) final String groupId,
			@RequestParam("id") final String id) {
		schedulerManager.pauseJob(groupId, id);
		return response(null);
	}

	/**
	 * Resume a specific paused job.
	 * 
	 * @param groupId
	 *        the group ID of the job to resume
	 * @param id
	 *        the ID of the job to resume
	 * @return the response
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs/resume", method = RequestMethod.POST)
	public Response<Void> resumeJob(
			@RequestParam(value = "groupId", required = false) final String groupId,
			@RequestParam("id") final String id) {
		schedulerManager.resumeJob(groupId, id);
		return response(null);
	}

	/**
	 * Update the scheduler's status.
	 * 
	 * @param desiredStatus
	 *        the desired status of the scheduler
	 * @return the response
	 */
	@ResponseBody
	@RequestMapping(value = "/status", method = RequestMethod.POST)
	public Response<Void> updateStatus(@RequestParam("status") final SchedulerStatus desiredStatus) {
		schedulerManager.updateStatus(desiredStatus);
		return response(null);
	}

	/**
	 * Get the scheduler's configured jobs.
	 * 
	 * @param filter
	 *        an optional filter to restrict the results to; if not provided all
	 *        jobs are returned
	 * @return the list of jobs
	 */
	@ResponseBody
	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public Response<Collection<JobInfo>> listJobs(final JobFilter filter) {
		Collection<JobInfo> infos = schedulerManager.allJobInfos();
		if ( filter != null ) {
			for ( Iterator<JobInfo> itr = infos.iterator(); itr.hasNext(); ) {
				JobInfo info = itr.next();
				if ( !filter.includesJobInfo(info) ) {
					itr.remove();
				}
			}
		}
		return response(infos);
	}

}
