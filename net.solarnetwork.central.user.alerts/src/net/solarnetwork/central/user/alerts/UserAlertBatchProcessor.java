/* ==================================================================
 * UserAlertBatchProcessor.java - 15/05/2015 7:26:03 pm
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

package net.solarnetwork.central.user.alerts;

import net.solarnetwork.central.user.domain.UserAlert;
import org.joda.time.DateTime;

/**
 * API for batch processing user alerts.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserAlertBatchProcessor {

	/**
	 * Process a batch of alerts, optionally starting from the ID of the last
	 * alert processed.
	 * 
	 * @param lastProcessedAlertId
	 *        An optional {@link UserAlert} ID representing the last ID
	 *        processed on a previous batch run.
	 * @param validDate
	 *        The valid date to use when batch processing alerts.
	 * @return The ID of the last {@link UserAlert} processed, or <em>null</em>
	 *         if no more alerts are available to process.
	 */
	Long processAlerts(Long lastProcessedAlertId, DateTime validDate);

}
