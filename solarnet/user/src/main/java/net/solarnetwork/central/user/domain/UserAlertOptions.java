/* ==================================================================
 * UserAlertOptions.java - 17/05/2015 8:24:27 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

/**
 * {@link UserAlert} option constants.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserAlertOptions {

	/**
	 * An age threshold, in the form of a decimal number of seconds.
	 */
	static String AGE_THRESHOLD = "age";

	/**
	 * A list of string datum source ID values.
	 */
	static String SOURCE_IDS = "sourceIds";

	/**
	 * A list of time window objects.
	 */
	static String TIME_WINDOWS = "windows";

}
