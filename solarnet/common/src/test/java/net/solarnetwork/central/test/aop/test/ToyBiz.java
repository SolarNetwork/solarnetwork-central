/* ==================================================================
 * ToyBiz.java - 22/09/2026 8:39:25 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test.aop.test;

/**
 * A toy service API for testing the security contract support.
 *
 * @author matt
 * @version 1.0
 */
public interface ToyBiz {

	/**
	 * Read something for a user.
	 *
	 * @param userId
	 *        the user ID
	 * @return the thing
	 */
	String userThing(Long userId);

	/**
	 * Save something for a user.
	 *
	 * @param userId
	 *        the user ID
	 * @param value
	 *        the value
	 */
	void saveUserThing(Long userId, String value);

	/**
	 * Read something for a node.
	 *
	 * @param nodeId
	 *        the node ID
	 * @return the thing
	 */
	String nodeThing(Long nodeId);

	/**
	 * Save something for a node.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param value
	 *        the value
	 */
	void saveNodeThing(Long nodeId, String value);

	/**
	 * Read something for a user, without any security.
	 *
	 * @param userId
	 *        the user ID
	 * @return the thing
	 */
	String unguardedThing(Long userId);

	/**
	 * Read something public.
	 *
	 * @return the thing
	 */
	String publicThing();

}
