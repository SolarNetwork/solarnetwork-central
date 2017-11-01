/* ==================================================================
 * MaintenanceSubscriber.java - 1/11/2017 1:50:00 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.util.Map;
import net.solarnetwork.domain.Identifiable;

/**
 * API for a service that needs periodic maintenance performed.
 * 
 * <p>
 * This API is meant to allow a service to subscribe to a periodic task or job
 * in order to maintain implementation specific resources over time. For example
 * a service may wish to purge expired items from a cache periodically.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.36
 */
public interface MaintenanceSubscriber extends Identifiable {

	/**
	 * Perform any required periodic maintenance.
	 * 
	 * @param parameters
	 *        job parameters, never {@literal null}
	 */
	void performServiceMaintenance(Map<String, ?> parameters);

}
