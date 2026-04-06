/* ==================================================================
 * UserDatumExportTaskPK.java - 18/04/2018 9:15:15 AM
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

package net.solarnetwork.central.user.datum.export.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * Primary key for a user export task.
 *
 * @author matt
 * @version 1.1
 */
public class UserDatumExportTaskPK
		implements Serializable, Cloneable, Comparable<UserDatumExportTaskPK> {

	@Serial
	private static final long serialVersionUID = -517540846705509888L;

	private Long userId;
	private Instant date;
	private ScheduleType scheduleType;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param scheduleType
	 *        the schedule type
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDatumExportTaskPK(Long userId, ScheduleType scheduleType, Instant date) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.scheduleType = requireNonNullArgument(scheduleType, "scheduleType");
		this.date = requireNonNullArgument(date, "date");
	}

	@Override
	public String toString() {
		return "UserDatumExportTaskPK{userId=" + userId + ",scheduleType=" + scheduleType + ",date="
				+ date + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((scheduleType == null) ? 0 : scheduleType.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || !(obj instanceof UserDatumExportTaskPK other) ) {
			return false;
		}

		// compare dates ignoring time zone differences here
		if ( date == null ) {
			if ( other.date != null ) {
				return false;
			}
		} else if ( other.date == null ) {
			return false;
		} else if ( date.compareTo(other.date) != 0 ) {
			return false;
		}

		if ( scheduleType != other.scheduleType ) {
			return false;
		}
		if ( userId == null ) {
			return other.userId == null;
		}
		return userId.equals(other.userId);
	}

	/**
	 * Compare two {@code GeneralNodeDatumPK} objects.
	 *
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 *
	 * <ol>
	 * <li>userId</li>
	 * <li>scheduleType</li>
	 * <li>date</li>
	 * </ol>
	 *
	 * <p>
	 * {@code null} values will be sorted before non-{@code null} values.
	 * </p>
	 */
	@Override
	public int compareTo(@Nullable UserDatumExportTaskPK o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.userId == null ) {
			return 1;
		} else if ( userId == null ) {
			return -1;
		}
		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.scheduleType == null ) {
			return 1;
		} else if ( scheduleType == null ) {
			return -1;
		}
		comparison = scheduleType.compareTo(o.scheduleType);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.date == null ) {
			return 1;
		} else if ( date == null ) {
			return -1;
		}
		return date.compareTo(o.date);
	}

	@Override
	public UserDatumExportTaskPK clone() {
		try {
			return (UserDatumExportTaskPK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	public final Long getUserId() {
		return userId;
	}

	public final void setUserId(Long userId) {
		this.userId = userId;
	}

	public final Instant getDate() {
		return date;
	}

	public final void setDate(Instant date) {
		this.date = date;
	}

	public final ScheduleType getScheduleType() {
		return scheduleType;
	}

	public final void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}

	public final char getScheduleTypeKey() {
		ScheduleType type = getScheduleType();
		if ( type == null ) {
			type = ScheduleType.Daily;
		}
		return type.getKey();
	}

	public final void setScheduleTypeKey(char key) {
		ScheduleType type;
		try {
			type = ScheduleType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			type = ScheduleType.Daily;
		}
		setScheduleType(type);
	}

}
