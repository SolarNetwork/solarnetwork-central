/* ==================================================================
 * CapacityGroupTaskContext.java - 1/09/2022 5:28:12 pm
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

package net.solarnetwork.central.oscp.util;

import java.time.Instant;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;

/**
 * Extension of {@link TaskContext} to add capacity group related information.
 * 
 * @author matt
 * @version 1.0
 */
public interface CapacityGroupTaskContext<C extends BaseOscpExternalSystemConfiguration<C>>
		extends TaskContext<C> {

	/**
	 * Get the capacity group identifier.
	 * 
	 * @return the group
	 */
	String groupIdentifier();

	/**
	 * Get the task date, which is task specific.
	 * 
	 * @return the task date
	 */
	Instant taskDate();

}
