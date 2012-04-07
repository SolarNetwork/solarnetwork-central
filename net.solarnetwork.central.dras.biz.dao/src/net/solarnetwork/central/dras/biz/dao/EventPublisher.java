/* ==================================================================
 * EventPublisher.java - Jun 29, 2011 8:50:50 PM
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.dras.biz.EventExecutor;
import net.solarnetwork.central.dras.biz.EventExecutor.EventExecutionReceipt;
import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.EventExecutionInfoDao;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventExecutionInfo;
import net.solarnetwork.central.dras.domain.EventExecutionTargets;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.util.OptionalServiceTracker;

import org.joda.time.DateTime;
import org.osgi.service.event.EventAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publish an event to a EventExecutor service.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class EventPublisher extends JobSupport {

	private TransactionTemplate transactionTemplate;
	private EventDao eventDao;
	private EventExecutionInfoDao eventExecutionInfoDao;
	private OptionalServiceTracker<EventExecutor> eventExecutor;
	
	/**
	 * Constructor.
	 * 
	 * @param eventDao the EventDao
	 */
	@Autowired
	public EventPublisher(EventDao eventDao, EventExecutionInfoDao eventExecutionInfoDao,
			TransactionTemplate transactionTemplate, 
			OptionalServiceTracker<EventExecutor> eventExecutor,
			EventAdmin eventAdmin) {
		super(eventAdmin);
		this.eventDao = eventDao;
		this.eventExecutionInfoDao = eventExecutionInfoDao;
		this.transactionTemplate = transactionTemplate;
		this.eventExecutor = eventExecutor;
	}
	
	@Override
	protected boolean handleJob(org.osgi.service.event.Event job)
			throws Exception {
		log.debug("Got Event publish job notification {}", job.getTopic());
		
		final Long eventId = (Long)job.getProperty("eventId");
		if ( eventId == null ) {
			log.warn("Event ID not found on publish job properties, ignoring");
			return false;
		}

		EventExecutionInfo info = transactionTemplate.execute(
				new TransactionCallback<EventExecutionInfo>() {
			@Override
			public EventExecutionInfo doInTransaction(TransactionStatus status) {
				final Event drasEvent = eventDao.get(eventId);
				if ( drasEvent == null ) {
					log.warn("Event {} not available, ignoring", eventId);
					return null;
				}
				
				EventExecutionInfo info = new EventExecutionInfo();
				info.setEvent(drasEvent);
				info.getParticipants().addAll(eventDao.resolveUserMembers(eventId, null));
				
				Set<EventExecutionTargets> eventTargetsMembers = eventDao.getEventExecutionTargets(eventId, null);
				List<EventExecutionTargets> eventTargets 
					= new ArrayList<EventExecutionTargets>(eventTargetsMembers);
				info.setTargets(eventTargets);
				// TODO modes
				
				info.setId(eventExecutionInfoDao.store(info));
				log.debug("Stored EventExecutionInfo {} for event {}", info.getId(), eventId);
				return info;
			}
		});
		if ( info == null ) {
			return false;
		}
		
		
		EventExecutor executor = eventExecutor.getService();
		if ( executor == null ) {
			log.warn("No EventExectuor service available, ignoring event ID {}", eventId);
			return false;
		}
		final EventExecutionReceipt receipt = executor.executeEvent(info);
		if ( receipt == null ) {
			log.warn("No EventExecutionReceipt returned by EventExecutor");
			return false;
		}
		
		info.setExecutionKey(receipt.getExecutionId());
		info.setExecutionDate(new DateTime());
		eventExecutionInfoDao.store(info);
		return true;
	}

}
