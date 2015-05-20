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

/**
 * API for user alert tasks.
 * 
 * @author matt
 * @version 1.0
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

}
