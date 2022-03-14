/* ==================================================================
 * BasicJobInfo.java - 24/01/2018 3:23:38 PM
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

package net.solarnetwork.central.scheduler;

import java.time.Instant;

/**
 * Basic implementation of {@link JobInfo}.
 * 
 * @author matt
 * @version 2.0
 * @since 1.37
 */
public class BasicJobInfo implements JobInfo {

	private final String groupId;
	private final String id;
	private final String executionScheduleDescription;

	public BasicJobInfo(String groupId, String id, String executionScheduleDescription) {
		super();
		this.groupId = groupId;
		this.id = id;
		this.executionScheduleDescription = executionScheduleDescription;
	}

	@Override
	public final String getGroupId() {
		return groupId;
	}

	@Override
	public final String getId() {
		return id;
	}

	@Override
	public final String getExecutionScheduleDescription() {
		return executionScheduleDescription;
	}

	@Override
	public JobStatus getJobStatus() {
		return null;
	}

	@Override
	public boolean isExecuting() {
		return false;
	}

	@Override
	public Instant getPreviousExecutionTime() {
		return null;
	}

	@Override
	public Instant getNextExecutionTime() {
		return null;
	}

}
