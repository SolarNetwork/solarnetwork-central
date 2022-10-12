/* ==================================================================
 * WebUtils.java - 15/08/2022 3:24:54 pm
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

package net.solarnetwork.central.web;

import java.net.URI;
import java.util.Map;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Helper utilities for web APIs.
 * 
 * @author matt
 * @version 1.0
 */
public final class WebUtils {

	private WebUtils() {
		// not allowed
	}

	/**
	 * Build a {@link UriComponents} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariableValues
	 *        the optional URI variable values
	 * @return the URI components
	 */
	public static UriComponents withoutHost(UriComponentsBuilder builder, Object... uriVariableValues) {
		return builder.scheme(null).host(null).port(null).buildAndExpand(uriVariableValues);
	}

	/**
	 * Build a {@link UriComponents} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariables
	 *        the optional URI variables
	 * @return the URI components
	 */
	public static UriComponents withoutHost(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		return builder.scheme(null).host(null).port(null).buildAndExpand(uriVariables);
	}

	/**
	 * Build a {@link URI} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariableValues
	 *        the optional URI variable values
	 * @return the URI
	 */
	public static URI uriWithoutHost(UriComponentsBuilder builder, Object... uriVariableValues) {
		return withoutHost(builder, uriVariableValues).toUri();
	}

	/**
	 * Build a {@link URI} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariables
	 *        the optional URI variables
	 * @return the URI
	 */
	public static URI uriWithoutHost(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		return withoutHost(builder, uriVariables).toUri();
	}

}
