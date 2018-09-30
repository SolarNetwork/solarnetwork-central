/* ==================================================================
 * CachedContent.java - 1/10/2018 7:25:48 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;

/**
 * Simple cached content item.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCachedContent implements CachedContent {

	private static final long serialVersionUID = 7168846971070309662L;

	private final HttpHeaders headers;
	private final byte[] data;
	private final String contentEncoding;

	/**
	 * Constructor.
	 * 
	 * @param headers
	 *        the headers
	 * @param data
	 *        the data
	 * @param contentEncoding
	 *        the content encoding, or {@literal null}
	 */
	public SimpleCachedContent(HttpHeaders headers, byte[] data, String contentEncoding) {
		super();
		this.headers = headers;
		this.data = data;
		this.contentEncoding = contentEncoding;
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	@Override
	public String getContentEncoding() {
		return contentEncoding;
	}

	@Override
	public InputStream getContent() throws IOException {
		return (data != null ? new ByteArrayInputStream(data) : null);
	}

	@Override
	public long getContentLength() {
		return (data != null ? data.length : 0);
	}

}
