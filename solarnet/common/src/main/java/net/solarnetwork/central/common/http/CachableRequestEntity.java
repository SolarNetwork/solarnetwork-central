/* ==================================================================
 * CachableRequestEntity.java - 13/3/2025 2:13:24 pm
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

package net.solarnetwork.central.common.http;

import java.net.URI;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.util.ObjectUtils;

/**
 * Extension of {@link RequestEntity} that changes the equality methods to
 * consider a "context" object (such as a user ID) and headers in addition to
 * the method and URI.
 * 
 * @author matt
 * @version 1.1
 */
public final class CachableRequestEntity extends RequestEntity<Void> {

	private Object context;

	/**
	 * Constructor.
	 *
	 * @param context
	 *        the context, such as a user ID
	 * @param headers
	 *        the headers
	 * @param method
	 *        the method
	 * @param url
	 *        the URL
	 */
	public CachableRequestEntity(Object context, MultiValueMap<String, String> headers,
			HttpMethod method, URI url) {
		this(context, headers != null ? new HttpHeaders(headers) : null, method, url);
		this.context = ObjectUtils.requireNonNullArgument(context, "context");
	}

	/**
	 * Constructor.
	 *
	 * @param context
	 *        the context, such as a user ID
	 * @param headers
	 *        the headers
	 * @param method
	 *        the method
	 * @param url
	 *        the URL
	 * @since 1.1
	 */
	public CachableRequestEntity(Object context, HttpHeaders headers, HttpMethod method, URI url) {
		super(headers, method, url);
		this.context = ObjectUtils.requireNonNullArgument(context, "context");
	}

	@Override
	public boolean equals(Object other) {
		if ( !super.equals(other) ) {
			return false;
		}
		CachableRequestEntity o = (CachableRequestEntity) other;
		return Objects.equals(context, o.context) && Objects.equals(getHeaders(), o.getHeaders());
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result |= Objects.hash(context, getHeaders());
		return result;
	}

}
