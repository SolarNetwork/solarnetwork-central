/* ==================================================================
 * JobFilter.java - 26/01/2018 7:33:31 AM
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

package net.solarnetwork.central.jobs.web.domain;

import net.solarnetwork.central.scheduler.JobInfo;

/**
 * A filter for job queries.
 * 
 * @author matt
 * @version 1.0
 */
public class JobFilter {

	private String groupId;
	private String id;
	private Boolean executing;

	public String getGroupId() {
		return groupId;
	}

	/**
	 * Set the job group ID to restrict the results to.
	 * 
	 * @param groupId
	 *        the ID of the job group to restrict results to
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getId() {
		return id;
	}

	/**
	 * Set the job ID to restrict results to.
	 * 
	 * @param id
	 *        the ID of the job to restrict results to
	 */
	public void setId(String id) {
		this.id = id;
	}

	public Boolean getExecuting() {
		return executing;
	}

	/**
	 * Set flag to restrict results based on job executing status.
	 * 
	 * @param executing
	 *        if {@literal true} then restrict results to executing jobs; if
	 *        {@literal false} then restrict results to idle jobs; if
	 *        {@literal null} then do not filter based on executing status
	 *        (include all jobs)
	 */
	public void setExecuting(Boolean executing) {
		this.executing = executing;
	}

	/**
	 * Test if this filter includes a given job.
	 * 
	 * @param info
	 *        the job to test against
	 * @return {@literal true} if this filter does not exclude the given job,
	 *         {@literal false} otherwise
	 */
	public boolean includesJobInfo(JobInfo info) {
		if ( id != null && !id.equalsIgnoreCase(info.getId()) ) {
			return false;
		}
		if ( groupId != null && !groupId.equalsIgnoreCase(info.getGroupId()) ) {
			return false;
		}
		if ( executing != null && executing.booleanValue() != info.isExecuting() ) {
			return false;
		}
		return true;
	}

}
