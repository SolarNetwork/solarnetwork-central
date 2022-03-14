/* ==================================================================
 * ContainerMetadata.java - 21/02/2022 9:42:04 AM
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

package net.solarnetwork.central.cloud.domain;

import net.solarnetwork.util.ObjectUtils;

/**
 * General metadata about a container application.
 * 
 * @author matt
 * @version 1.0
 */
public class ContainerMetadata {

	private final String containerId;

	/**
	 * Constructor.
	 * 
	 * @param containerId
	 *        the unique container ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ContainerMetadata(String containerId) {
		super();
		this.containerId = ObjectUtils.requireNonNullArgument(containerId, "containerId");
	}

	/**
	 * Get the unique container ID.
	 * 
	 * @return the container ID, never {@literal null}
	 */
	public String getContainerId() {
		return containerId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ContainerMetadata{");
		builder.append("containerId=").append(containerId);
		builder.append("}");
		return builder.toString();
	}

}
