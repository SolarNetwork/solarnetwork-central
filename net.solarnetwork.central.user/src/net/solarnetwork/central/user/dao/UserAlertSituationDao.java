/* ==================================================================
 * UserAlertSituationDao.java - 16/05/2015 4:21:42 pm
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
import net.solarnetwork.central.user.domain.UserAlertSituation;

/**
 * DAO API for UserAlertSituation objects.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserAlertSituationDao extends GenericDao<UserAlertSituation, Long> {

	/**
	 * Get an {@link UserAlertSituation} that is active for a given
	 * {@link UserAlert} ID. If more than one are active, this will return the
	 * most recent one only.
	 * 
	 * @param alertId
	 *        The ID of the {@link UserAlert} to get the active situations for.
	 * @return The found {@link UserAlertSituation}, or <em>null</em> if none
	 *         available.
	 */
	UserAlertSituation getActiveAlertSituationForAlert(Long alertId);

	List<UserAlert> findActiveAlertSituationsForUser(Long userId);

	List<UserAlert> findActiveAlertSituationsForNode(Long nodeId);

}
