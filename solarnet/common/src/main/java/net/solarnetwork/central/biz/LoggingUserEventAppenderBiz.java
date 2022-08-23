/* ==================================================================
 * LoggingUserEventAppenderBiz.java - 23/08/2022 3:07:36 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.TimeBasedV7UuidGenerator;

/**
 * {@link UserEventAppenderBiz} that simply logs events.
 * 
 * @author matt
 * @version 1.0
 */
public class LoggingUserEventAppenderBiz implements UserEventAppenderBiz {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UuidGenerator uuidGenerator = TimeBasedV7UuidGenerator.INSTANCE;

	@Override
	public UserEvent addEvent(Long userId, LogEventInfo info) {
		UserEvent event = new UserEvent(userId, uuidGenerator.generate(),
				requireNonNullArgument(info, "info").getTags(), info.getMessage(), info.getData());
		log.info("USER EVENT: {}", event);
		return event;
	}

}
