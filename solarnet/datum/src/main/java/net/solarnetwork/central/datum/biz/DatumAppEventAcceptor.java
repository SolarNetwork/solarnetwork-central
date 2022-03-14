/* ==================================================================
 * DatumAppEventAcceptor.java - 29/05/2020 4:23:44 pm
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

package net.solarnetwork.central.datum.biz;

import net.solarnetwork.central.datum.domain.DatumAppEvent;

/**
 * API for a service that can accept datum events in a durable, persistent
 * manner.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public interface DatumAppEventAcceptor {

	/**
	 * Offer a datum event.
	 * 
	 * @param event
	 *        the event to accept
	 * @throws IllegalArgumentException
	 *         if {@code event} is {@literal null}
	 */
	void offerDatumEvent(DatumAppEvent event);

}
