/* ==================================================================
 * BasicDatumStreamMetadata.java - 22/10/2020 3:07:55 pm
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
import java.util.UUID;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Implementation of {@link DatumStreamMetadata}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicDatumStreamMetadata implements DatumStreamMetadata, Serializable {

	private static final long serialVersionUID = -6339837836211488890L;

	private final UUID streamId;
	private final String timeZoneId;
	private final String[] instantaneousProperties;
	private final String[] accumulatingProperties;
	private final String[] statusProperties;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId} are allowed to be {@literal null}.
	 * If any array is empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@literal null}
	 */
	public BasicDatumStreamMetadata(UUID streamId, String timeZoneId, String[] instantaneousProperties,
			String[] accumulatingProperties, String[] statusProperties) {
		super();
		if ( streamId == null ) {
			throw new IllegalArgumentException("The streamId argument must not be null.");
		}
		this.streamId = streamId;
		this.timeZoneId = timeZoneId;
		this.instantaneousProperties = instantaneousProperties != null
				&& instantaneousProperties.length > 0 ? instantaneousProperties : null;
		this.accumulatingProperties = accumulatingProperties != null && accumulatingProperties.length > 0
				? accumulatingProperties
				: null;
		this.statusProperties = statusProperties != null && statusProperties.length > 0
				? statusProperties
				: null;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId} are allowed to be {@literal null}.
	 * The other arguments are {@code Object} to work around MyBatis mapping
	 * issues. If any array is empty, it will be treated as if it were
	 * {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param instantaneousProperties
	 *        the instantaneous property names; must be a {@code String[]}
	 * @param accumulatingProperties
	 *        the accumulating property names; must be a {@code String[]}
	 * @param statusProperties
	 *        the status property names; must be a {@code String[]}
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@literal null}
	 */
	public BasicDatumStreamMetadata(UUID streamId, String timeZoneId, Object instantaneousProperties,
			Object accumulatingProperties, Object statusProperties) {
		this(streamId, timeZoneId, (String[]) instantaneousProperties, (String[]) accumulatingProperties,
				(String[]) statusProperties);
	}

	@Override
	public UUID getStreamId() {
		return streamId;
	}

	@Override
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Get the total number of instantaneous, accumulating, and status property
	 * names.
	 * 
	 * @return the total number of properties
	 */
	public int getPropertyNamesLength() {
		return getInstantaneousLength() + getAccumulatingLength() + getStatusLength();
	}

	@Override
	public String[] getPropertyNames() {
		final int iLen = getInstantaneousLength();
		final int aLen = getAccumulatingLength();
		final int sLen = getStatusLength();
		final int len = iLen + aLen + sLen;
		if ( len < 1 ) {
			return null;
		}
		String[] result = new String[len];
		if ( iLen > 0 ) {
			System.arraycopy(instantaneousProperties, 0, result, 0, iLen);
		}
		if ( aLen > 0 ) {
			System.arraycopy(accumulatingProperties, 0, result, iLen, aLen);
		}
		if ( sLen > 0 ) {
			System.arraycopy(statusProperties, 0, result, iLen + aLen, sLen);
		}
		return result;
	}

	@Override
	public String[] propertyNamesForType(GeneralDatumSamplesType type) {
		if ( type == null ) {
			return null;
		}
		switch (type) {
			case Instantaneous:
				return instantaneousProperties;

			case Accumulating:
				return accumulatingProperties;

			case Status:
				return statusProperties;

			default:
				return null;
		}
	}

	/**
	 * Get the instantaneous property names array length.
	 * 
	 * @return the number of instantaneous property names
	 */
	public int getInstantaneousLength() {
		return (instantaneousProperties != null ? instantaneousProperties.length : 0);
	}

	/**
	 * Get the accumulating property names array length.
	 * 
	 * @return the number of accumulating property names
	 */
	public int getAccumulatingLength() {
		return (accumulatingProperties != null ? accumulatingProperties.length : 0);
	}

	/**
	 * Get the status property names array length.
	 * 
	 * @return the number of status property names
	 */
	public int getStatusLength() {
		return (statusProperties != null ? statusProperties.length : 0);
	}

}
