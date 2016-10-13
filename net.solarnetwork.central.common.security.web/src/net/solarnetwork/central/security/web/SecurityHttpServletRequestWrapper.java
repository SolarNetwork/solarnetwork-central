/* ==================================================================
 * SecurityHttpServletRequestWrapper.java - Oct 4, 2014 3:54:59 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.codec.digest.DigestUtils;
import net.solarnetwork.central.security.SecurityException;

/**
 * {@link HttpServletRequestWrapper} to aid in computing hash values for the
 * request content.
 * 
 * @author matt
 * @version 1.1
 */
public class SecurityHttpServletRequestWrapper extends HttpServletRequestWrapper {

	private final int maximumLength;
	private boolean requestBodyCached;
	private byte[] cachedRequestBody; // TODO: support writing to temp file if body > maximumLength!

	/**
	 * Construct from a request.
	 * 
	 * @param request
	 *        the request to wrap
	 */
	public SecurityHttpServletRequestWrapper(HttpServletRequest request, int maxLength) {
		super(request);
		this.maximumLength = maxLength;
	}

	private void cacheRequestBody() throws IOException {
		if ( requestBodyCached ) {
			return;
		}
		requestBodyCached = true;
		InputStream in = super.getInputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		try {
			int byteCount = 0;
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ( (bytesRead = in.read(buffer)) != -1 ) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
				if ( byteCount > this.maximumLength ) {
					throw new net.solarnetwork.central.security.SecurityException(
							"Request body too large.");
				}
			}
			out.flush();
			cachedRequestBody = out.toByteArray();
		} finally {
			try {
				in.close();
			} catch ( IOException ex ) {
			}
			try {
				out.close();
			} catch ( IOException ex ) {
			}
		}
	}

	/**
	 * Compute the MD5 hash of the request body.
	 * 
	 * @return the MD5 hash, or <em>null</em> if there is no request content
	 * @throws IOException
	 *         if an IO exception occurs
	 * @throws SecurityException
	 *         if the request content length is larger than the configured
	 *         {@code maximumLength}
	 */
	public byte[] getContentMD5() throws IOException {
		cacheRequestBody();
		if ( cachedRequestBody == null || cachedRequestBody.length < 1 ) {
			return null;
		}
		return DigestUtils.md5(cachedRequestBody);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if ( requestBodyCached ) {
			return new ServletInputStream() {

				final private ByteArrayInputStream byis = new ByteArrayInputStream(cachedRequestBody);

				@Override
				public int read() throws IOException {
					return byis.read();
				}
			};
		}
		return super.getInputStream();
	}

}
