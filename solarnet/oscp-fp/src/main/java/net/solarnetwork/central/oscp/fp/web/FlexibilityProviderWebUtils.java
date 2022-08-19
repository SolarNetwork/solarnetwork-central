/* ==================================================================
 * FlexibilityProviderWebUtils.java - 19/08/2022 1:27:15 pm
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

package net.solarnetwork.central.oscp.fp.web;

import java.util.concurrent.CompletableFuture;

/**
 * Web utilities for the Flexibility Provider.
 * 
 * @author matt
 * @version 1.0
 */
public final class FlexibilityProviderWebUtils {

	/**
	 * A thread-local for dealing with "do something after returning response to
	 * client" pattern in OSCP.
	 */
	public static final ThreadLocal<CompletableFuture<Void>> RESPONSE_SENT = new ThreadLocal<>();

}
