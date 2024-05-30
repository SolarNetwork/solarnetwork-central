/* ==================================================================
 * UserServiceAuditor.java - 29/05/2024 4:34:35 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz;

import java.time.Clock;

/**
 * API for auditing user service events in SolarNetwork.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserServiceAuditor {

	/**
	 * Get the clock used for auditing.
	 * 
	 * <p>
	 * This clock may bucket time into discreet intervals.
	 * </p>
	 * 
	 * @return the clock never {@literal null}
	 */
	Clock getAuditClock();

	/**
	 * Audit a service for a user.
	 * 
	 * @param userId
	 *        the user ID
	 * @param service
	 *        the service name
	 * @param count
	 *        the amount to add
	 */
	void auditUserService(Long userId, String service, int count);

}
