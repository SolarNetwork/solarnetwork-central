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
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;

/**
 * DAO API for UserAlert objects.
 * 
 * @author matt
 * @version 1.1
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
	 * @param validDate
	 *        A timestamp to use for the validity check date. If
	 *        {@code startingId} is provided, this value can be provided to
	 *        issue a stable batch query based on the same valid date as the
	 *        previous call to this method. If not provided the current time
	 *        will be used, but then a subsequent batch call might not have the
	 *        same date if another batch call is needed. Therefore it is
	 *        recommended to always pass a value for this parameter.
	 * @param max
	 *        An optional maximum number of result rows to return.
	 * @return The found alerts, or an empty list if none found.
	 */
	List<UserAlert> findAlertsToProcess(UserAlertType type, Long startingId, DateTime validDate,
			Integer max);

	/**
	 * Get a set of all alerts configured for a user. The alerts will have the
	 * most recently available active {@link UserAlertSituation} populated on
	 * the {@link UserAlert#getSituation()} property.
	 * 
	 * @param userId
	 *        The ID of the user to get all alerts for.
	 * @return The found alerts, or an empty list if none found.
	 */
	List<UserAlert> findAlertsForUser(Long userId);

	/**
	 * Delete all alerts configured for a given user and node.
	 * 
	 * @param userId
	 *        The ID of the owner of the alerts.
	 * @param nodeId
	 *        The ID of the node.
	 * @return The count of alerts deleted.
	 * @since 1.1
	 */
	int deleteAllAlertsForNode(Long userId, Long nodeId);

	/**
	 * Get a specific alert with the most recently available active
	 * {@link UserAlertSituation} populated on the
	 * {@link UserAlert#getSituation()} property.
	 * 
	 * @param alertId
	 *        The ID of the alert to get.
	 * @return The found alert, or <em>null</em> if not available.
	 */
	UserAlert getAlertSituation(Long alertId);

	/**
	 * Update the {@code validTo} property to a new date.
	 * 
	 * @param alertId
	 *        The ID of the alert to update.
	 * @param validTo
	 *        The new value for the {@code validTo} property.
	 * @since 1.1
	 */
	void updateValidTo(Long alertId, DateTime validTo);

}
