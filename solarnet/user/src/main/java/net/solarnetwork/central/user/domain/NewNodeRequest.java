/* ==================================================================
 * NewNodeRequest.java - Mar 26, 2013 10:17:02 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import java.util.Locale;
import java.util.TimeZone;

/**
 * Bean for a new node request details.
 * 
 * @author matt
 * @version 1.0
 */
public class NewNodeRequest {

	private final Long userId;
	private final String securityPhrase;
	private final TimeZone timeZone;
	private final Locale locale;

	/**
	 * Construct with values.
	 * 
	 * @param userId
	 *        the user to associate the node with
	 * @param securityPhrase
	 *        the security phrase to use during the association process
	 * @param timeZone
	 *        a time zone for the node
	 * @param locale
	 *        a locale for the node; at least the country should be specified
	 */
	public NewNodeRequest(Long userId, String securityPhrase, TimeZone timeZone, Locale locale) {
		super();
		this.userId = userId;
		this.securityPhrase = securityPhrase;
		this.timeZone = timeZone;
		this.locale = locale;
	}

	public Long getUserId() {
		return userId;
	}

	public String getSecurityPhrase() {
		return securityPhrase;
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public Locale getLocale() {
		return locale;
	}

}
