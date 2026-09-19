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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Primary key for a datum stream.
 *
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumPK extends StreamPK implements Serializable, Cloneable, Comparable<DatumPK> {

	@Serial
	private static final long serialVersionUID = 1829112080933789997L;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the time stamp
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumPK(UUID streamId, Instant timestamp) {
		super(streamId, timestamp);
	}

	@Override
	public DatumPK clone() {
		return (DatumPK) super.clone();
	}

	/**
	 * Compare two key objects.
	 *
	 * <p>
	 * This compares stream ID values followed by timestamp values. Both are
	 * ordered in ascending order with {@code null} values ordered last.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(@Nullable DatumPK o) {
		return super.compareWith(o);
	}

	/**
	 * Test if the stream ID is assigned.
	 *
	 * @return {@code true} if {@link #getStreamId()} is not {@code null} and
	 *         not equal to {@link StreamDatum#UNASSIGNED_STREAM_ID}
	 * @see StreamDatum#isStreamIdAssigned(UUID)
	 */
	public boolean streamIdIsAssigned() {
		return StreamDatum.isStreamIdAssigned(getStreamId());
	}

}
