/* ==================================================================
 * UserOutputConfiguration.java - 21/03/2018 2:13:25 PM
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

package net.solarnetwork.central.user.export.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.datum.domain.export.OutputCompressionType;
import net.solarnetwork.central.datum.domain.export.OutputConfiguration;

/**
 * User related {@link OutputConfiguration} entity.
 * 
 * @author matt
 * @version 1.0
 */
public class UserOutputConfiguration extends BaseExportConfigurationEntity
		implements OutputConfiguration, Serializable {

	private static final long serialVersionUID = -1581617729654201770L;

	private OutputCompressionType compressionType;

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
	@JsonIgnore
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
