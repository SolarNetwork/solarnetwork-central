/* ==================================================================
 * EntityNotificationListener.java - Jun 29, 2011 9:43:40 AM
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

package net.solarnetwork.central.dras.biz.alert;

import static net.solarnetwork.central.dras.biz.Notifications.ACTING_USER_IDENTITY;
import static net.solarnetwork.central.dras.biz.Notifications.ENTITY_CHANGE_TYPE;
import static net.solarnetwork.central.dras.biz.Notifications.ENTITY_CLASS_NAME;
import static net.solarnetwork.central.dras.biz.Notifications.ENTITY_IDENTITY;

import java.util.Map;

import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.dras.biz.AlertBiz;
import net.solarnetwork.central.dras.biz.Notifications;
import net.solarnetwork.central.dras.biz.Notifications.EntityChangeType;
import net.solarnetwork.central.dras.support.SimpleAlert;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi {@link EventHandler} designed to work with 
 * {@link Notifications#TOPIC_ENTITY_UPDATE} notifications and send
 * out alerts using an {@link AlertBiz}.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>alertBiz</dt>
 *   <dd>The {@link AlertBiz} to post alerts with.</dd>
 *   
 *   <dt>alertTypeMapping</dt>
 *   <dd>A mapping of {@link EntityChangeType} keys to
 *   associated {@link AlertBiz.Alert#getAlertType()} values to use for
 *   posted alerts. The {@link Notifications#ENTITY_CHANGE_TYPE} values
 *   are assumed to be String values of {@link EntityChangeType} values.<dd>
 *   
 *   <dt>daoMapping</dt>
 *   <dd>A mapping of {@link Notifications#ENTITY_CLASS_NAME} keys to
 *   associated DAO implementations for acquiring entities based on the
 *   {@link Notifications#ENTITY_IDENTITY} notification property.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class EntityNotificationListener implements EventHandler {

	private AlertBiz alertBiz;
	private Map<EntityChangeType, String> alertTypeMapping;
	private Map<String, GenericDao<? extends Entity<Long>, Long>> daoMapping;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void handleEvent(Event event) {
		log.debug("Handling OSGi event {}", event.getTopic());
		
		final Long actorId = (Long)event.getProperty(ACTING_USER_IDENTITY);
		
		final Long entityId = (Long)event.getProperty(ENTITY_IDENTITY);
		if ( entityId == null ) {
			log.debug("Ignoring OSGi event {} missing property {}", 
					event.getTopic(), ENTITY_IDENTITY);
			return;
		}
		
		final String changeTypeName = (String)event.getProperty(ENTITY_CHANGE_TYPE);
		if ( changeTypeName == null ) {
			log.debug("Ignoring OSGi event {} missing property {}",
					event.getTopic(), ENTITY_CHANGE_TYPE);
		}
		
		final EntityChangeType changeType = EntityChangeType.valueOf(changeTypeName);
		final String alertType = alertTypeMapping.get(changeType);
		if ( alertType == null ) {
			throw new IllegalArgumentException("EntityChangeType " +changeType 
					+" is not supported.");
		}
		
		final String entityType = (String)event.getProperty(ENTITY_CLASS_NAME);
		if ( !daoMapping.containsKey(entityType) ) {
			throw new IllegalArgumentException("Entity " +entityType 
					+" is not supported.");
		}
		GenericDao<? extends Entity<Long>, Long> dao = daoMapping.get(entityType);
		final Entity<Long> entity = dao.get(entityId);
		
		SimpleAlert alert = new SimpleAlert();
		alert.setAlertType(alertType);
		alert.setRegardingIdentity(entity);
		alert.setActorId(actorId);
		alertBiz.postAlert(alert);
	}

	public AlertBiz getAlertBiz() {
		return alertBiz;
	}
	public void setAlertBiz(AlertBiz alertBiz) {
		this.alertBiz = alertBiz;
	}
	public Map<String, GenericDao<? extends Entity<Long>, Long>> getDaoMapping() {
		return daoMapping;
	}
	public void setDaoMapping(
			Map<String, GenericDao<? extends Entity<Long>, Long>> daoMapping) {
		this.daoMapping = daoMapping;
	}
	public Map<EntityChangeType, String> getAlertTypeMapping() {
		return alertTypeMapping;
	}
	public void setAlertTypeMapping(Map<EntityChangeType, String> alertTypeMapping) {
		this.alertTypeMapping = alertTypeMapping;
	}
	
}
