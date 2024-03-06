/* ==================================================================
 * UserEventAppenderBiz.java - 1/08/2022 3:25:32 pm
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

package net.solarnetwork.central.biz;

import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;

/**
 * Service API for appending user events.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserEventAppenderBiz {

	/**
	 * Add an event.
	 * 
	 * @param userId
	 *        the user account ID
	 * @param info
	 *        the event info to add
	 * @return the generated event
	 */
	UserEvent addEvent(Long userId, LogEventInfo info);

	/**
	 * Helper function to add an event to an optional appender.
	 * 
	 * <p>
	 * If {@code biz} is {@literal null}, this method simply returns
	 * {@literal null}.
	 * </p>
	 * 
	 * @param biz
	 *        the optional appender
	 * @param userId
	 *        the user account ID
	 * @param info
	 *        the event info to add
	 * @return the generated event, or {@literal null} if {@code biz} is
	 *         {@literal null}
	 */
	static UserEvent addEvent(UserEventAppenderBiz biz, Long userId, LogEventInfo info) {
		if ( biz == null ) {
			return null;
		}
		return biz.addEvent(userId, info);
	}

}
