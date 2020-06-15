/* ==================================================================
 * DatumAppEvent.java - 29/05/2020 3:56:32 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.AppEvent;

/**
 * A datum-specific event.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public interface DatumAppEvent extends AppEvent {

	/**
	 * Get the node ID the event is for.
	 * 
	 * @return the node ID, never {@literal null}
	 */
	Long getNodeId();

	/**
	 * Get the source ID the event is for.
	 * 
	 * @return the source ID, never {@literal null}
	 */
	String getSourceId();

}
