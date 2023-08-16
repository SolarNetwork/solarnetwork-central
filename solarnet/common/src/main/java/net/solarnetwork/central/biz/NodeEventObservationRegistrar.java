/* ==================================================================
 * ConfigurableNodeObserver.java - 10/08/2023 6:53:03 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.util.function.Consumer;

/**
 * API for a registry of node-based event observers.
 * 
 * @param <T>
 *        the observed object type
 * @author matt
 * @version 1.0
 */
public interface NodeEventObservationRegistrar<T> {

	/**
	 * Register a node observer.
	 * 
	 * @param observer
	 *        the observer to register
	 * @param nodeIds
	 *        the IDs of the nodes to observe
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void registerNodeObserver(Consumer<T> observer, Long... nodeIds);

	/**
	 * Unregister a node observer.
	 * 
	 * @param observer
	 *        the observer to unregister
	 * @param nodeIds
	 *        the IDs of the node to stop observing, or {@literal null} to stop
	 *        observing all node IDs
	 */
	void unregisterNodeObserver(Consumer<T> observer, Long... nodeIds);

}
