/* ==================================================================
 * AlertBiz.java - Jun 18, 2011 5:01:35 PM
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

package net.solarnetwork.central.dras.biz;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Future;

import net.solarnetwork.central.domain.Identity;

/**
 * API for system alerts.
 * 
 * @author matt
 * @version $Revision$
 */
public interface AlertBiz {

	/** Alert type when an Entity is created. */
	String ALERT_TYPE_ENTITY_CREATED = "EntityCreated";
	
	/** Alert type when an Entity is modified. */
	String ALERT_TYPE_ENTITY_MODIFIED = "EntityModified";

	interface Alert extends Serializable {
		
		/**
		 * Get a key that identifies the type of alert.
		 * 
		 * @return type
		 */
		String getAlertType();
		
		/**
		 * Get a brief description of the alert.
		 * 
		 * @return description
		 */
		String getDescription();
		
		/**
		 * Get the full alert message.
		 * 
		 * @return
		 */
		String getMessageBody();
		
		/**
		 * Get the identity of something the alert is in regards to,
		 * e.g. Event, Program.
		 * 
		 * @return identity
		 */
		Identity<Long> getRegardingIdentity();
		
		/**
		 * Get the alert recipient identifiers, e.g. User, Participant.
		 * 
		 * @return recipients
		 */
		Set<Identity<Long>> getRecipients();
		
		/**
		 * Get the ID of the acting user.
		 * 
		 * @return user ID
		 */
		Long getActorId();
	}
	
	interface AlertProcessingResult extends Serializable {
		
		/**
		 * Get the alert that was processed.
		 * 
		 * @return the Alert
		 */
		Alert getAlert();
		
		/**
		 * Get the set of users that were notified by the alert.
		 * 
		 * @return set of user identities
		 */
		Set<Identity<Long>> getAlertedUsers();
		
	}
	
	/**
	 * Send an alert.
	 * 
	 * @param alert the alert to send
	 */
	Future<AlertProcessingResult> postAlert(Alert alert);
	
}
