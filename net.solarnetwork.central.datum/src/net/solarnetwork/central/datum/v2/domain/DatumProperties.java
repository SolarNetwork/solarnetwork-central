/* ==================================================================
 * DatumSamples.java - 22/10/2020 10:38:32 am
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
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * A collection of property values for a datum.
 * 
 * <p>
 * The properties are stored as ordered arrays of values. The meaning of the
 * values depends on external {@link DatumStreamMetadata}. {@literal null}
 * values are allowed both as the array fields of this class and as values
 * within array instances.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DatumProperties implements Serializable {

	private static final long serialVersionUID = -2647856276610023629L;

	private BigDecimal[] instantaneous;
	private BigDecimal[] accumulating;
	private String[] status;
	private String[] tags;

	/**
	 * Create stample instance.
	 * 
	 * @param instantaneous
	 *        the instantaneous values
	 * @param accumulating
	 *        the accumulating values
	 * @param status
	 *        the status values
	 * @param tags
	 *        the tag values
	 * @return the samples instance, never {@literal null}
	 */
	public static DatumProperties propertiesOf(BigDecimal[] instantaneous, BigDecimal[] accumulating,
			String[] status, String[] tags) {
		DatumProperties s = new DatumProperties();
		s.instantaneous = instantaneous;
		s.accumulating = accumulating;
		s.status = status;
		s.tags = tags;
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(accumulating);
		result = prime * result + Arrays.hashCode(instantaneous);
		result = prime * result + Arrays.hashCode(status);
		result = prime * result + Arrays.hashCode(tags);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof DatumProperties) ) {
			return false;
		}
		DatumProperties other = (DatumProperties) obj;
		return Arrays.equals(accumulating, other.accumulating)
				&& Arrays.equals(instantaneous, other.instantaneous)
				&& Arrays.equals(status, other.status) && Arrays.equals(tags, other.tags);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DatumProperties{");
		if ( instantaneous != null ) {
			builder.append("instantaneous=");
			builder.append(Arrays.toString(instantaneous));
			builder.append(", ");
		}
		if ( accumulating != null ) {
			builder.append("accumulating=");
			builder.append(Arrays.toString(accumulating));
			builder.append(", ");
		}
		if ( status != null ) {
			builder.append("status=");
			builder.append(Arrays.toString(status));
			builder.append(", ");
		}
		if ( tags != null ) {
			builder.append("tags=");
			builder.append(Arrays.toString(tags));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the overall number of array property values.
	 * 
	 * <p>
	 * This returns the sum of all the array fields of this class.
	 * </p>
	 * 
	 * @return the number of values (including {@literal null} values)
	 */
	public int getLength() {
		return getInstantaneousLength() + getAccumulatingLength() + getStatusLength() + getTagsLength();
	}

	/**
	 * Get the instantaneous values array length.
	 * 
	 * @return the number of instantaneous values (including {@literal null}
	 *         values)
	 */
	public int getInstantaneousLength() {
		BigDecimal[] array = getInstantaneous();
		return (array != null ? array.length : 0);
	}

	/**
	 * Get the instantaneous values.
	 * 
	 * @return the instantaneous sample values
	 */
	public BigDecimal[] getInstantaneous() {
		return instantaneous;
	}

	/**
	 * Set the instantaneous values.
	 * 
	 * @param values
	 *        the values to set
	 */
	public void setInstantaneous(BigDecimal[] values) {
		this.instantaneous = values;
	}

	/**
	 * Get the accumulating values array length.
	 * 
	 * @return the number of accumulating values (including {@literal null}
	 *         values)
	 */
	public int getAccumulatingLength() {
		BigDecimal[] array = getInstantaneous();
		return (array != null ? array.length : 0);
	}

	/**
	 * Get the accumulating values.
	 * 
	 * @return the accumulating sample values
	 */
	public BigDecimal[] getAccumulating() {
		return accumulating;
	}

	/**
	 * Set the accumulating values.
	 * 
	 * @param values
	 *        the values to set
	 */
	public void setAccumulating(BigDecimal[] values) {
		this.accumulating = values;
	}

	/**
	 * Get the status values array length.
	 * 
	 * @return the number of status values (including {@literal null} values)
	 */
	public int getStatusLength() {
		String[] array = getStatus();
		return (array != null ? array.length : 0);
	}

	/**
	 * Get the status values.
	 * 
	 * @return the status sample values
	 */
	public String[] getStatus() {
		return status;
	}

	/**
	 * Set the status values.
	 * 
	 * @param values
	 *        the values to set
	 */
	public void setStatus(String[] status) {
		this.status = status;
	}

	/**
	 * Get the tags array length.
	 * 
	 * @return the number of tags (including {@literal null} values)
	 */
	public int getTagsLength() {
		BigDecimal[] array = getInstantaneous();
		return (array != null ? array.length : 0);
	}

	/**
	 * Get the tag values.
	 * 
	 * @return the tag values
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * Set the tag values.
	 * 
	 * @param tags
	 *        the tags to set
	 */
	public void setTags(String[] tags) {
		this.tags = tags;
	}

}
