/* ==================================================================
 * HttpDestinationProperties.java - 20/03/2024 7:43:26 am
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

package net.solarnetwork.central.datum.export.dest.http;

import java.util.Map;
import org.springframework.http.HttpMethod;
import net.solarnetwork.util.StringUtils;

/**
 * Service properties for the HTTP export destination.
 *
 * @author matt
 * @version 1.0
 */
public class HttpDestinationProperties {

	/** The {@code method} property default value. */
	public static final HttpMethod DEFAULT_METHOD = HttpMethod.POST;

	private String method = DEFAULT_METHOD.toString();
	private String url;
	private String multipartFilenameTemplate;
	private Map<String, String> headers;

	/**
	 * Constructor.
	 */
	public HttpDestinationProperties() {
		super();
	}

	/**
	 * Test if the configuration appears valid.
	 *
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (url != null && !url.isBlank());
	}

	/**
	 * Get the method as an {@link HttpMethod} instance.
	 *
	 * @return the instance, never {@literal null}
	 */
	public HttpMethod method() {
		try {
			return HttpMethod.valueOf(method);
		} catch ( Exception e ) {
			// ignore
		}
		return DEFAULT_METHOD;
	}

	/**
	 * Get the HTTP method.
	 *
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Set the HTTP method.
	 *
	 * @param method
	 *        the method to set, for example {@code POST} or {@code PUT}
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Get the HTTP URL.
	 *
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the HTTP URL.
	 *
	 * @param url
	 *        the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the multipart filename template.
	 *
	 * @return the multipart filename template
	 */
	public String getMultipartFilenameTemplate() {
		return multipartFilenameTemplate;
	}

	/**
	 * Set the multipart filename template.
	 *
	 * <p>
	 * Configuring this setting turns the HTTP request into a multipart request.
	 * </p>
	 *
	 * @param multipartFilenameTemplate
	 *        the multipart filename template to use, or {@literal null} for a
	 *        normal HTTP request
	 */
	public void setMultipartFilenameTemplate(String multipartFilenameTemplate) {
		this.multipartFilenameTemplate = multipartFilenameTemplate;
	}

	/**
	 * Test if the {@code multipartFilenameTemplate} is configured.
	 *
	 * @return {@literal true} if {@code multipartFilenameTemplate} is
	 *         configured
	 */
	public boolean isMultipart() {
		return (multipartFilenameTemplate != null && !multipartFilenameTemplate.isBlank());
	}

	/**
	 * Get the extra HTTP request headers to include.
	 *
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Set extra HTTP request headers to include.
	 *
	 * @param headers
	 *        the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	/**
	 * Get the extra HTTP request headers as a delimited string.
	 *
	 * <p>
	 * The key-value delimited used is {@code :} and the pair delimited is
	 * {@code ,}.
	 * </p>
	 *
	 * @return the headers as a delimited string
	 */
	public String getHeadersValue() {
		return StringUtils.delimitedStringFromMap(headers, ": ", ", ");
	}

	/**
	 * Set the extra HTTP request headers as a delimited string.
	 *
	 * <p>
	 * The key-value delimited used is {@code :} and the pair delimited is
	 * {@code ,}.
	 * </p>
	 *
	 * @param value
	 *        the headers to set
	 */
	public void setHeadersValue(String value) {
		this.headers = StringUtils.delimitedStringToMap(value, ",", ":");
	}

}
