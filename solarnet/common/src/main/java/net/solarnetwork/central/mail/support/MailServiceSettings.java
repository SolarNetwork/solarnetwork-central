/* ==================================================================
 * MailServiceSettings.java - 6/09/2025 6:58:51 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.mail.support;

import java.util.Map;

/**
 * Settings for a mail service.
 * 
 * @author matt
 * @version 1.0
 */
public class MailServiceSettings {

	private Map<String, String> headers;

	/**
	 * Get static headers to apply to all messages.
	 * 
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Set static headers to apply to all messages.
	 * 
	 * @param headers
	 *        the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

}
