/* ==================================================================
 * Notifications.java - Jun 29, 2011 11:14:22 AM
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

/**
 * Constants for system notifications.
 * 
 * <p>These constants are used for system notifications, modeled after
 * OSGi EventAdmin notifications where notifications have a <em>topic</em>
 * and associated <em>properties</em>.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public final class Notifications {

	/** The notification topic for when an Entity changes. */
	public static final String TOPIC_ENTITY_UPDATE = "net/solarnetwork/central/dras/ENTITY_UPDATE";
	
	/** The notification property for the class name of the posting service. */
	public static final String SERVICE_CLASS_NAME = "BizClassName";
	
	/** The notification property for the ID of the subject of the event. */
	public static final String ENTITY_IDENTITY = "Identity";
	
	/** The notification property for the class of the entity of the event. */
	public static final String ENTITY_CLASS_NAME = "ClassName";
	
	/** The notification property for the type of change. */
	public static final String ENTITY_CHANGE_TYPE = "ChangeType";

	/** The notification property for the active user initiating the event. */
	public static final String ACTING_USER_IDENTITY = "Actor";
	
	/** Possible values for the {@link #ENTITY_CHANGE_TYPE} property. */
	public enum EntityChangeType {
		Created,
		Modified,
		MembershipUpdated;
	}
	
	// can't create me
	private Notifications() {
		super();
	}
	
}
