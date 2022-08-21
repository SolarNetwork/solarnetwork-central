/* ==================================================================
 * ExternalSystemSupportDao.java - 21/08/2022 4:00:36 pm
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

package net.solarnetwork.central.oscp.dao;

import java.time.Instant;
import java.util.function.Function;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;

/**
 * DAO API for supporting external system processes.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemSupportDao {

	/**
	 * Lay claim to an external system who needs to have a heartbeat sent.
	 * 
	 * @param handler
	 *        a function that will be passed the ID of an external system
	 *        configuration who needs to have a heartbeat sent, and returns a
	 *        new heartbeat date if a heartbeat was successfully sent, or
	 *        {@literal null} otherwise
	 * @return {@literal true} if the heartbeat date was updated with the value
	 *         returned from {@code handler}
	 */
	boolean processExternalSystemWithExpiredHeartbeat(Function<AuthRoleInfo, Instant> handler);

}
