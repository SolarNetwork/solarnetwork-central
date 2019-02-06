/* ==================================================================
 * BasicOutputConfiguration.java - 21/03/2018 11:19:10 AM
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

package net.solarnetwork.central.datum.export.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;

/**
 * Basic implementation of {@link OutputConfiguration}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.23
 */
@JsonPropertyOrder({ "name", "serviceIdentifier", "compressionTypeKey", "serviceProperties" })
public class BasicOutputConfiguration extends BasicIdentifiableConfiguration
		implements OutputConfiguration, Serializable {

	private static final long serialVersionUID = -588365600656134370L;

	private OutputCompressionType compressionType;

	/**
	 * Default constructor.
	 */
	public BasicOutputConfiguration() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the configuration to copy
	 * @since 1.1
	 */
	public BasicOutputConfiguration(OutputConfiguration other) {
		super(other);
		setCompressionType(other.getCompressionType());
	}

	@JsonIgnore
	@Override
	public OutputCompressionType getCompressionType() {
		return compressionType;
	}

	public void setCompressionType(OutputCompressionType compressionType) {
		this.compressionType = compressionType;
	}

	/**
	 * Get the compression type key value.
	 * 
	 * @return the compression type; if {@link #getCompressionType()} is
	 *         {@literal null} this will return the key value for
	 *         {@link OutputCompressionType#None}
	 */
	public char getCompressionTypeKey() {
		OutputCompressionType type = getCompressionType();
		return (type != null ? type.getKey() : OutputCompressionType.None.getKey());
	}

	/**
	 * Set the output compression type via its key value.
	 * 
	 * @param key
	 *        the key of the compression type to set; if {@code key} is
	 *        unsupported, the compression will be set to
	 *        {@link OutputCompressionType#None}
	 */
	public void setCompressionTypeKey(char key) {
		OutputCompressionType type = OutputCompressionType.None;
		try {
			type = OutputCompressionType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			// ignore, and force to None
		}
		setCompressionType(type);
	}

}
