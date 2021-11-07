/* ==================================================================
 * JobKey.java - 7/11/2021 4:45:55 PM
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

package net.solarnetwork.central.scheduler;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Objects;

/**
 * A basic job key.
 * 
 * @author matt
 * @version 1.0
 */
public final class JobKey implements Comparable<JobKey> {

	private final String groupId;
	private final String id;

	/**
	 * Constructor.
	 * 
	 * @param groupId
	 *        the group ID
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JobKey(String groupId, String id) {
		super();
		this.groupId = requireNonNullArgument(groupId, "groupId");
		this.id = requireNonNullArgument(id, "id");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JobKey{");
		if ( groupId != null )
			builder.append("groupId=").append(groupId).append(", ");
		if ( id != null )
			builder.append("id=").append(id);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int compareTo(JobKey o) {
		int result = groupId.compareTo(o.groupId);
		if ( result == 0 ) {
			result = id.compareTo(o.id);
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, id);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof JobKey) ) {
			return false;
		}
		JobKey other = (JobKey) obj;
		return Objects.equals(groupId, other.groupId) && Objects.equals(id, other.id);
	}

	/**
	 * Get the group ID.
	 * 
	 * @return the group ID.
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Get the job ID.
	 * 
	 * @return the job ID
	 */
	public String getId() {
		return id;
	}

}
