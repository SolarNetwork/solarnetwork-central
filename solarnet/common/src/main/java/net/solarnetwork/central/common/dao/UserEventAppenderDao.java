/* ==================================================================
 * UserEventAppenderDao.java - 1/08/2022 2:25:17 pm
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

package net.solarnetwork.central.common.dao;

import net.solarnetwork.central.domain.UserEvent;

/**
 * DAO API for write-only access to creating user events.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserEventAppenderDao {

	/**
	 * Add an event.
	 * 
	 * @param event
	 *        the event to add
	 */
	void add(UserEvent event);

}
