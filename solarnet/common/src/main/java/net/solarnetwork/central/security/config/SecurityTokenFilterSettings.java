/* ==================================================================
 * SecurityTokenFilterSettings.java - 4/03/2022 10:10:35 AM
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

package net.solarnetwork.central.security.config;

import org.springframework.util.unit.DataSize;

/**
 * Configurable settings for security token filters.
 * 
 * @author matt
 * @version 1.0
 */
public class SecurityTokenFilterSettings {

	/** The {@code maxDateSkew} property default value. */
	public static final long DEFALUT_MAX_DATE_SKEW = 15 * 60 * 1000;

	public static final DataSize DEFAULT_MAX_REQUEST_BODY_SIZE = DataSize.ofBytes(65535);

	private long maxDateSkew = DEFALUT_MAX_DATE_SKEW;
	private DataSize maxRequestBodySize = DEFAULT_MAX_REQUEST_BODY_SIZE;

	/**
	 * Get the maximum date skew.
	 * 
	 * @return the maximum date skew, in milliseconds; defaults to
	 *         {@link #DEFALUT_MAX_DATE_SKEW}
	 */
	public long getMaxDateSkew() {
		return maxDateSkew;
	}

	/**
	 * Set the maximum date skew.
	 * 
	 * @param maxDateSkew
	 *        the maximum date skew, in milliseconds
	 */
	public void setMaxDateSkew(long maxDateSkew) {
		this.maxDateSkew = maxDateSkew;
	}

	/**
	 * Get the maximum request body size.
	 * 
	 * @return the maximum size, never {@literal null} ; defaults to
	 *         {@link #DEFAULT_MAX_REQUEST_BODY_SIZE}
	 */
	public DataSize getMaxRequestBodySize() {
		return maxRequestBodySize;
	}

	/**
	 * Set the maximum request body size.
	 * 
	 * @param maxRequestBodySize
	 *        the maximum size to set; if {@literal null} then
	 *        {@link #DEFAULT_MAX_REQUEST_BODY_SIZE} will be set
	 */
	public void setMaxRequestBodySize(DataSize maxRequestBodySize) {
		if ( maxRequestBodySize == null ) {
			maxRequestBodySize = DEFAULT_MAX_REQUEST_BODY_SIZE;
		}
		this.maxRequestBodySize = maxRequestBodySize;
	}

}
