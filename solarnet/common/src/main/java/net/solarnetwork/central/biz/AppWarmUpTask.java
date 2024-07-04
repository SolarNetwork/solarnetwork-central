/* ==================================================================
 * AppWarmUpTask.java - 3/07/2024 4:52:43 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

/**
 * API for a task to perform immediately after the application starts up.
 * 
 * @author matt
 * @version 1.0
 */
@FunctionalInterface
public interface AppWarmUpTask {

	/** A qualifier or profile for warm-up tasks. */
	String WARMUP = "warmup";

	/** An identifier string to use in place of a real identifier. */
	String IDENT = "APP.WARMUP";

	/**
	 * Perform the warm-up task.
	 * 
	 * @throws Exception
	 *         if any error occurs
	 */
	void warmUp() throws Exception;

}
