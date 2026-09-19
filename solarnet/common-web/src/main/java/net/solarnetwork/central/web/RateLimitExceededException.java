/* ==================================================================
 * RateLimitExceededException.java - 19/04/2025 10:40:30 am
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

package net.solarnetwork.central.web;

/**
 * A rate-limit exceeded exception.
 *
 * @author matt
 * @version 1.0
 */
public class RateLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = 7561950062522492971L;

	private final String key;
	private final Long id;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the rate limit key
	 * @param id
	 *        the rate limit ID
	 */
	public RateLimitExceededException(String key, Long id) {
		super();
		this.key = key;
		this.id = id;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the ID.
	 *
	 * @return the ID
	 */
	public Long getId() {
		return id;
	}

}
