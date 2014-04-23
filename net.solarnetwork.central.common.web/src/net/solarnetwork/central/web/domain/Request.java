/* ==================================================================
 * Request.java - Nov 20, 2012 7:04:41 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.domain;

import java.util.Map;

/**
 * A request envelope object.
 * 
 * @author matt
 * @version 1.1
 * @deprecated use the net.solarnetwork.web.domain.Request class directly
 */
@Deprecated
public class Request extends net.solarnetwork.web.domain.Request {

	public Request(String username, String password, Map<String, Object> data) {
		super(username, password, data);
	}

}
