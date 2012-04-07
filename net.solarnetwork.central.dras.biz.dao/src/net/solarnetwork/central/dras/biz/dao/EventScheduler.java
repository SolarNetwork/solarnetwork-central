/* ==================================================================
 * EventScheduler.java - Jun 29, 2011 8:54:12 PM
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

package net.solarnetwork.central.dras.biz.dao;

import static net.solarnetwork.central.dras.biz.Notifications.*;
import static net.solarnetwork.central.scheduler.SchedulerConstants.*;

import java.util.HashMap;
import java.util.Map;

import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.scheduler.EventHandlerSupport;

import org.joda.time.DateTime;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link EventHandler} for event change requests, to schedule
 * publication of the event at the event date.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class EventScheduler extends EventHandlerSupport {

	private EventAdmin eventAdmin;
	private EventDao eventDao;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin the EventAdmin
	 * @param eventDao the EventDao
	 */
	@Autowired
	public EventScheduler(EventAdmin eventAdmin, EventDao eventDao) {
		this.eventAdmin = eventAdmin;
		this.eventDao = eventDao;
	}
	
	@Override
	protected void handleEventInternal(org.osgi.service.event.Event event) {
		final Long eventId = (Long)event.getProperty(ENTITY_IDENTITY);
		
		Event drasEvent = eventDao.get(eventId);
		if ( drasEvent == null ) {
			log.warn("Event not found for ID {}, not scheduling job", eventId);
			return;
		}
		
		final DateTime publishDate;
		if ( drasEvent.getNotificationDate() != null 
				&& drasEvent.getNotificationDate().isAfterNow() ) {
			publishDate = drasEvent.getNotificationDate();
		} else if ( drasEvent.getEndDate().isAfterNow() ) {
			publishDate = new DateTime();
		} else {
			log.warn("Event {} ended in the past, or notification date not available",
					eventId);
			return;
		}
		
		Map<String, Object> jobProps = new HashMap<String, Object>(5);
		jobProps.put("eventId", eventId);
		
		// schedule publish job for future notification date
		Map<String, Object> publishProps = new HashMap<String, Object>(5);
		publishProps.put(JOB_TOPIC, "net/solarnetwork/central/dras/ddrs/PUBLISH_EVENT");
		publishProps.put(JOB_GROUP, "DDRS");
		publishProps.put(JOB_ID, "event:"+eventId);
		publishProps.put(JOB_DATE, publishDate.getMillis());
		publishProps.put(JOB_PROPERTIES, jobProps);
		
		org.osgi.service.event.Event publishJob = new org.osgi.service.event.Event(
				TOPIC_JOB_REQUEST, publishProps);
		eventAdmin.postEvent(publishJob);
	}

}
