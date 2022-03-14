/* ==================================================================
 * ApplicationMetadata.java - 21/02/2022 10:19:16 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central;

import java.util.UUID;
import net.solarnetwork.util.ObjectUtils;

/**
 * General application metadata.
 * 
 * @author matt
 * @version 1.0
 */
public class ApplicationMetadata {

	private final String name;
	private final String version;
	private final String instanceId;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the application name, must not be {@literal null}
	 * @param the
	 *        application version; if {@literal null} an empty string will be
	 *        set
	 * @param instanceId
	 *        a unique application instance ID, to differentiate multiple
	 *        instances of the same application; if {@literal null} a UUID-based
	 *        value will be assigned
	 * @throws IllegalArgumentException
	 *         if {@code name} is {@literal null}
	 */
	public ApplicationMetadata(String name, String version, String instanceId) {
		super();
		this.name = ObjectUtils.requireNonNullArgument(name, "name");
		this.version = (version != null ? version : "");
		this.instanceId = (instanceId != null && !instanceId.isBlank() ? instanceId
				: UUID.randomUUID().toString());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApplicationMetadata{");
		if ( name != null && !name.isBlank() ) {
			builder.append("name=").append(name).append(", ");
		}
		if ( version != null && !version.isBlank() ) {
			builder.append("version=").append(version).append(", ");
		}
		if ( instanceId != null && !instanceId.isBlank() ) {
			builder.append("instanceId=").append(instanceId);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the name.
	 * 
	 * @return the name, never {@litearl null}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Get the instance ID.
	 * 
	 * @return the instance ID, never {@literal null}
	 */
	public String getInstanceId() {
		return instanceId;
	}

}
