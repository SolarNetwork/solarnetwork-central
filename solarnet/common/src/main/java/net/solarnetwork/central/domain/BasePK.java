/* ==================================================================
 * BasePK.java - 11/04/2019 9:36:57 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.io.Serializable;
import org.apache.commons.codec.digest.DigestUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for primary key values.
 * 
 * @author matt
 * @version 1.0
 * @since 1.39
 */
public abstract class BasePK implements Serializable, Cloneable {

	private static final long serialVersionUID = 2726035205282491951L;

	/**
	 * Populate a string builder with an ID value.
	 * 
	 * <p>
	 * This method is called from {@link #getId()}.
	 * </p>
	 * 
	 * @param buf
	 *        the buffer to populate
	 */
	protected abstract void populateIdValue(StringBuilder buf);

	/**
	 * Get a computed string ID value for this primary key.
	 * 
	 * <p>
	 * Note this value is derived from the properties of this class, and not
	 * assigned by the system. This method calls
	 * {@link #populateIdValue(StringBuilder)} and then computes a hex-encoded
	 * SHA1 value from that as the final ID value.
	 * </p>
	 * 
	 * @return computed ID string
	 */
	@JsonIgnore
	public final String getId() {
		StringBuilder builder = new StringBuilder();
		populateIdValue(builder);
		return DigestUtils.sha1Hex(builder.toString());
	}

	/**
	 * Populate a string builder with a friendly string value.
	 * 
	 * <p>
	 * This method is called from {@link #toString()}. The buffer will be
	 * initially empty when invoked.
	 * </p>
	 * 
	 * @param buf
	 *        the buffer to populate
	 */
	protected abstract void populateStringValue(StringBuilder buf);

	/**
	 * Generate a string value.
	 * 
	 * <p>
	 * This method generates a string like <code>Class{data}</code> where
	 * {@code data} is generated via
	 * {@link #populateStringValue(StringBuilder)}.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		populateStringValue(builder);
		builder.insert(0, '{');
		builder.insert(0, getClass().getSimpleName());
		builder.append("}");
		return builder.toString();
	}

	@Override
	protected BasePK clone() {
		try {
			return (BasePK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

}
