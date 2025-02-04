/* ==================================================================
 * SchedulerAdminController.java - 9/11/2021 4:21:53 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.domain.Result.success;
import java.util.Collection;
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
import net.solarnetwork.domain.Result;

/**
 * REST controller for job scheduler management.
 *
 * @author matt
 * @version 1.0
 */
@RestController("v1SchedulerController")
@RequestMapping(value = { "/api/v1/sec/scheduler" })
public class SchedulerAdminController {

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
	public Result<SchedulerStatus> currentStatus() {
		return success(schedulerManager.currentStatus());
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
	public Result<Void> pauseJob(@RequestParam(value = "groupId", required = false) final String groupId,
			@RequestParam("id") final String id) {
		schedulerManager.pauseJob(groupId, id);
		return success();
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
	public Result<Void> resumeJob(
			@RequestParam(value = "groupId", required = false) final String groupId,
			@RequestParam("id") final String id) {
		schedulerManager.resumeJob(groupId, id);
		return success();
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
	public Result<Void> updateStatus(@RequestParam("status") final SchedulerStatus desiredStatus) {
		schedulerManager.updateStatus(desiredStatus);
		return success();
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
	public Result<Collection<JobInfo>> listJobs(final JobFilter filter) {
		Collection<JobInfo> infos = schedulerManager.allJobInfos();
		if ( filter != null ) {
			infos.removeIf(info -> !filter.includesJobInfo(info));
		}
		return success(infos);
	}

}
