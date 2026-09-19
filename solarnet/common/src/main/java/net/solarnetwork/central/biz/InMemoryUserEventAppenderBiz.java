/* ==================================================================
 * InMemoryUserEventAppenderBiz.java - 1/12/2025 10:23:42 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;

/**
 * Implementation of {@link UserEventAppenderBiz} that saves events into an
 * in-memory list.
 * 
 * @author matt
 * @version 1.0
 */
public final class InMemoryUserEventAppenderBiz implements UserEventAppenderBiz {

	private final List<UserEvent> events;

	/**
	 * Constructor.
	 */
	public InMemoryUserEventAppenderBiz() {
		super();
		this.events = new ArrayList<>(8);
	}

	private final UuidGenerator uuidGenerator = TimeBasedV7UuidGenerator.INSTANCE_12BIT;

	@Override
	public UserEvent addEvent(Long userId, LogEventInfo info) {
		UserEvent event = new UserEvent(userId, uuidGenerator.generate(),
				requireNonNullArgument(info, "info").getTags(), info.getMessage(), info.getData());
		events.add(event);
		return event;
	}

	/**
	 * Get the events.
	 * 
	 * @return the events
	 */
	public List<UserEvent> getEvents() {
		return events;
	}

}
