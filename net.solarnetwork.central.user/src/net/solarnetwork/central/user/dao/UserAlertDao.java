/* ==================================================================
 * UserAlertDao.java - 15/05/2015 1:57:08 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.util.List;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertType;

/**
 * DAO API for UserAlert objects.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserAlertDao extends GenericDao<UserAlert, Long> {

	/**
	 * Find a set of alerts that need processing. The results are sorted by ID
	 * in ascending order.
	 * 
	 * @param type
	 *        The type of alert to find.
	 * @param startingId
	 *        An optional {@link UserAlert} ID value to start from. Only alerts
	 *        with an ID value <em>higher</em> than this ID will be considered.
	 *        If <em>null</em> then consider all alerts.
	 * @param max
	 *        An optional maximum number of result rows to return.
	 * @return The found alerts, or an empty list if none found.
	 */
	List<UserAlert> findAlertsToProcess(UserAlertType type, Long startingId, Integer max);

}
