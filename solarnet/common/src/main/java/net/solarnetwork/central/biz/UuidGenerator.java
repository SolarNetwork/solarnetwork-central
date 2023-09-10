/* ==================================================================
 * UuidGenerator.java - 2/08/2022 5:30:52 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.util.UUID;

/**
 * API for a service that generates UUID values.
 * 
 * @author matt
 * @version 1.0
 * @deprecated use {@code net.solarnetwork.util.UuidGenerator} as a drop-in
 *             replacement
 */
@FunctionalInterface
@Deprecated(since = "1.17")
public interface UuidGenerator {

	/**
	 * Generate a new UUID value.
	 * 
	 * @return the new UUID
	 */
	UUID generate();

}
