/* ==================================================================
 * DelegatingEventBiz.java - Jun 24, 2011 3:28:10 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.biz.support;

import java.util.List;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.biz.EventBiz;
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Match;

/**
 * Delegating {@link EventBiz}, to support AOP with OSGi services.
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingEventBiz implements EventBiz {

	private EventBiz delegate;

	/**
	 * Construct with delegate.
	 * 
	 * @param delegate the delegate
	 */
	public DelegatingEventBiz(EventBiz delegate) {
		this.delegate = delegate;
	}

	@Override
	public Event getEvent(Long eventId) {
		return delegate.getEvent(eventId);
	}

	@Override
	public List<Match> findEvents(ObjectCriteria<EventFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findEvents(criteria, sortDescriptors);
	}

}
