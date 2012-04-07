/* ==================================================================
 * EntityChangeNotificationAspect.java - Jun 28, 2011 3:02:14 PM
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

package net.solarnetwork.central.dras.aop;

import static net.solarnetwork.central.dras.biz.Notifications.*;

import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Aspect for publishing OSGi Event Admin events.
 * 
 * @author matt
 * @version $Revision$
 */
@Aspect
public class EntityChangeNotificationAspect extends SecurityAspectSupport {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private EventAdmin eventAdmin;
	
	/**
	 * Constructor.
	 * 
	 * @param alertBiz the AlertBiz
	 */
	@Autowired
	public EntityChangeNotificationAspect(EventAdmin eventAdmin, UserDao userDao) {
		super(userDao);
		this.eventAdmin = eventAdmin;
	}
	
	@Pointcut(value="bean(aop*) && execution(* net.solarnetwork.central.dras.biz.*AdminBiz.*(..))")
	public void eventModify() {}
	
	/**
	 * Post an OSGi Event when a DRAS entity is created or modified.
	 * 
	 * @param jp the JoinPoint
	 * @param input the DRAS entity input
	 * @param output the DRAS entity output
	 */
	@AfterReturning(value = "eventModify() && args(input)", returning = "output")
	public void eventUpdate(JoinPoint jp, Entity<Long> input, Entity<Long> output) {
		Map<String, Object> props = createEntityProps(output, (input.getId() == null 
				? EntityChangeType.Created : EntityChangeType.Modified));
		props.put(SERVICE_CLASS_NAME, getServiceClassName(jp));
		postEvent(TOPIC_ENTITY_UPDATE, props);
	}

	/**
	 * Post an OSGi Event when a DRAS entity's membership is modified.
	 * 
	 * @param jp the JoinPoint
	 * @param output the DRAS EffectiveCollection
	 */
	@AfterReturning(value = "eventModify()", returning = "output")
	public void eventUpdateNotification(JoinPoint jp, 
			EffectiveCollection<? extends Entity<Long>, Member> output) {
		if ( output.getEffective() == null ) {
			// the membership didn't actually change, so don't post notification
			log.debug("{} membership unchanged, not posting OSGi Event", 
					output.getObject().getClass().getName());
			return;
		}
		Map<String, Object> props = createEntityProps(
				output.getObject(), EntityChangeType.MembershipUpdated);
		props.put(SERVICE_CLASS_NAME, getServiceClassName(jp));
		postEvent(TOPIC_ENTITY_UPDATE, props);
	}
	
	private void postEvent(String topicName, Map<String, Object> props) {
		log.debug("Posting OSGi Event {} with props {}", topicName, props);
		org.osgi.service.event.Event note = new org.osgi.service.event.Event(topicName, props);
		this.eventAdmin.postEvent(note);
	}
	
	private Map<String, Object> createEntityProps(Entity<Long> entity, EntityChangeType change) {
		Map<String, Object> props = new LinkedHashMap<String, Object>();
		props.put(ENTITY_IDENTITY, entity.getId());
		props.put(ENTITY_CLASS_NAME, entity.getClass().getName());
		props.put(ENTITY_CHANGE_TYPE, change.toString());
		props.put(ACTING_USER_IDENTITY, getCurrentUserId());
		return props;
	}

	private String getServiceClassName(JoinPoint jp) {
		Class<?> targetClass = jp.getTarget().getClass();
		Class<?>[] interfaces = targetClass.getInterfaces();
		String className;
		if ( interfaces != null && interfaces.length > 0 ) {
			className = interfaces[0].getName();
		} else {
			className = targetClass.getName();
		}
		return className;
	}

}
