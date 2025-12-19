/* ==================================================================
 * BasicUserNodeInstructionTaskFilter.java - 16/11/2025 2:38:32 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.ClaimableJobStateCriteria;
import net.solarnetwork.central.common.dao.TaskCriteria;
import net.solarnetwork.central.common.dao.TopicCriteria;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.ClaimableJobState;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserNodeInstructionTaskFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserNodeInstructionTaskFilter extends BasicCoreCriteria
		implements UserNodeInstructionTaskFilter {

	private String[] topics;
	private Long[] taskIds;
	private BasicClaimableJobState[] claimableJobStates;
	private Instant startDate;
	private Instant endDate;

	/**
	 * Constructor.
	 */
	public BasicUserNodeInstructionTaskFilter() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicUserNodeInstructionTaskFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public BasicUserNodeInstructionTaskFilter clone() {
		return (BasicUserNodeInstructionTaskFilter) super.clone();
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria == null ) {
			return;
		}
		if ( criteria instanceof BasicUserNodeInstructionTaskFilter f ) {
			setTopics(f.getTopics());
			setTaskIds(f.getTaskIds());
			setClaimableJobStates(f.getClaimableJobStates());
			setStartDate(f.getStartDate());
			setEndDate(f.getEndDate());
		} else {
			if ( criteria instanceof TopicCriteria c ) {
				setTopics(c.getTopics());
			}
			if ( criteria instanceof TaskCriteria c ) {
				setTaskIds(c.getTaskIds());
			}
			if ( criteria instanceof ClaimableJobStateCriteria c ) {
				var states = c.getClaimableJobStates();
				SequencedSet<BasicClaimableJobState> copy = null;
				if ( states != null && states.length > 0 ) {
					for ( ClaimableJobState s : states ) {
						if ( s instanceof BasicClaimableJobState j ) {
							if ( copy == null ) {
								copy = new LinkedHashSet<>(states.length);
							}
							copy.add(j);
						}
					}
				}
				setClaimableJobStates(copy.toArray(BasicClaimableJobState[]::new));
			}
			if ( criteria instanceof DateRangeCriteria c ) {
				setStartDate(c.getStartDate());
				setEndDate(c.getEndDate());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(claimableJobStates);
		result = prime * result + Arrays.hashCode(taskIds);
		result = prime * result + Arrays.hashCode(topics);
		result = prime * result + Objects.hash(endDate, startDate);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicUserNodeInstructionTaskFilter other) ) {
			return false;
		}
		return Arrays.equals(claimableJobStates, other.claimableJobStates)
				&& Objects.equals(endDate, other.endDate) && Objects.equals(startDate, other.startDate)
				&& Arrays.equals(taskIds, other.taskIds) && Arrays.equals(topics, other.topics);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicUserNodeInstructionTaskFilter{userIds=");
		builder.append(Arrays.toString(getUserIds()));
		if ( topics != null ) {
			builder.append(", topics=");
			builder.append(Arrays.toString(topics));
		}
		if ( taskIds != null ) {
			builder.append(", taskIds=");
			builder.append(Arrays.toString(taskIds));
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public boolean hasAnyCriteria() {
		// @formatter:off
		return     super.hasAnyCriteria()
				|| (claimableJobStates != null && claimableJobStates.length > 0)
				|| endDate != null
				|| startDate != null
				|| (taskIds != null && taskIds.length > 0)
				|| (topics != null && topics.length > 0)
				;
		// @formatter:on
	}

	@Override
	public String getTopic() {
		return UserNodeInstructionTaskFilter.super.getTopic();
	}

	/**
	 * Set the topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public void setTopic(String topic) {
		setTopics(topic != null ? new String[] { topic } : null);
	}

	@Override
	public String[] getTopics() {
		return topics;
	}

	/**
	 * Set the topics.
	 * 
	 * @param topics
	 *        the topics
	 */
	public void setTopics(String[] topics) {
		this.topics = topics;
	}

	@Override
	public Long getTaskId() {
		return UserNodeInstructionTaskFilter.super.getTaskId();
	}

	/**
	 * Set the task ID.
	 *
	 * @param taskId
	 *        the task ID to set
	 */
	public void setTaskId(Long taskId) {
		setTaskIds(taskId != null ? new Long[] { taskId } : null);
	}

	@Override
	public Long[] getTaskIds() {
		return taskIds;
	}

	/**
	 * Set the task IDs.
	 *
	 * @param taskIds
	 *        the task IDs to set
	 */
	public void setTaskIds(Long[] taskIds) {
		this.taskIds = taskIds;
	}

	@Override
	public final BasicClaimableJobState[] getClaimableJobStates() {
		return claimableJobStates;
	}

	/**
	 * Set the claimable job states.
	 *
	 * @param claimableJobStates
	 *        the states to set
	 */
	public final void setClaimableJobStates(BasicClaimableJobState[] claimableJobStates) {
		this.claimableJobStates = claimableJobStates;
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

}
