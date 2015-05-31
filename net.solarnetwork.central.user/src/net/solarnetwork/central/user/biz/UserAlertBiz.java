/* ==================================================================
 * UserAlertBiz.java - 19/05/2015 7:58:35 pm
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

package net.solarnetwork.central.user.biz;

import java.util.List;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;

/**
 * API for user alert tasks.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserAlertBiz {

	/**
	 * Get all available alerts for a given user. The
	 * {@link UserAlert#getSituation()} property will be populated with the most
	 * recently available <em>active</em> {@link UserAlertSituation}, if one
	 * exists.
	 * 
	 * @param userId
	 *        The ID of the user to get alerts for.
	 * @return List of alerts, or an empty list if none available.
	 */
	List<UserAlert> userAlertsForUser(Long userId);

	/**
	 * Save an alert. This method can be used to create new alerts or update
	 * existing alerts.
	 * 
	 * @param alert
	 *        The alert to save.
	 * @return The primary key of the saved alert.
	 */
	Long saveAlert(UserAlert alert);

	/**
	 * Get an alert with the most recently available <em>active</em>
	 * {@link UserAlertSituation} populated, if one exists.
	 * 
	 * @param alertId
	 *        The ID of the alert to get.
	 * @return The alert, or <em>null</em> if not available.
	 */
	UserAlert alertSituation(Long alertId);

	/**
	 * Update an alert <em>active</em> situation's status. If the given alert
	 * does not have an active situation, nothing will be updated.
	 * 
	 * @param alertId
	 *        The ID of the alert to update the situation status of.
	 * @param status
	 *        The status to update the situation to.
	 * @return The updated alert, or <em>null</em> if not available. The
	 *         {@link UserAlertSituation} will be populated, if one was updated.
	 */
	UserAlert updateSituationStatus(Long alertId, UserAlertSituationStatus status);

	/**
	 * Get a count of <em>active</em> alert situations for a given user.
	 * 
	 * @param userId
	 *        The ID of the user to get the alert situation count for.
	 * @return The number of active alert situations for the given user.
	 * @since 1.1
	 */
	int alertSituationCountForUser(Long userId);

	/**
	 * Get all available <em>active</em> alert situations for a given user. The
	 * {@link UserAlert#getSituation()} property will be populated with matching
	 * {@link UserAlertSituation}.
	 * 
	 * @param userId
	 *        The ID of the user to get alert situations for.
	 * @return List of alerts, or an empty list if none available.
	 * @since 1.1
	 */
	List<UserAlert> alertSituationsForUser(Long userId);

	/**
	 * Get all available <em>active</em> alert situations for a given node. The
	 * {@link UserAlert#getSituation()} property will be populated with matching
	 * {@link UserAlertSituation}.
	 * 
	 * @param nodeId
	 *        The ID of the node to get alert situations for.
	 * @return List of alerts, or an empty list if none available.
	 * @since 1.1
	 */
	List<UserAlert> alertSituationsForNode(Long nodeId);

}
