/* ==================================================================
 * DatumPK.java - 22/10/2020 8:55:47 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Primary key for a datum stream.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumPK extends StreamPK implements Serializable, Cloneable, Comparable<DatumPK> {

	private static final long serialVersionUID = 1829112080933789997L;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the time stamp
	 */
	public DatumPK(UUID streamId, Instant timestamp) {
		super(streamId, timestamp);
	}

	@Override
	protected DatumPK clone() {
		return (DatumPK) super.clone();
	}

	/**
	 * Compare two key objects.
	 * 
	 * <p>
	 * This compares stream ID values followed by timestamp values. Both are
	 * ordered in ascending order with {@literal null} values ordered last.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(DatumPK o) {
		return super.compareWith(o);
	}

}
