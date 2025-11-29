/* ==================================================================
 * ContentLengthTrackingClientHttpRequestInterceptor.java - 28/10/2024 5:53:50 pm
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

package net.solarnetwork.central.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import net.solarnetwork.util.ObjectUtils;

/**
 * Client HTTP request interceptor to track the number of bytes processed in the
 * response.
 * 
 * @author matt
 * @version 1.1
 */
public class ContentLengthTrackingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory
			.getLogger(ContentLengthTrackingClientHttpRequestInterceptor.class);

	private final ThreadLocal<AtomicLong> countThreadLocal;

	/**
	 * Constructor.
	 */
	public ContentLengthTrackingClientHttpRequestInterceptor(ThreadLocal<AtomicLong> countThreadLocal) {
		super();
		this.countThreadLocal = countThreadLocal;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		ClientHttpResponse response = execution.execute(request, body);
		return new ResponseBodyLengthTrackingClientHttpResponse(response);
	}

	private class ResponseBodyLengthTrackingClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse delegate;
		private BoundedInputStream countingBody;

		private ResponseBodyLengthTrackingClientHttpResponse(ClientHttpResponse delegate) {
			super();
			this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		}

		@Override
		public HttpHeaders getHeaders() {
			return delegate.getHeaders();
		}

		@Override
		public InputStream getBody() throws IOException {
			BoundedInputStream is = BoundedInputStream.builder().setInputStream(delegate.getBody())
					.get();
			countingBody = is;
			return is;
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return delegate.getStatusCode();
		}

		@SuppressWarnings("removal")
		@Override
		public int getRawStatusCode() throws IOException {
			return delegate.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return delegate.getStatusText();
		}

		@Override
		public void close() {
			delegate.close();
			final BoundedInputStream is = this.countingBody;
			if ( is != null ) {
				long count = is.getCount();
				if ( count > 0 ) {
					log.trace("Adding {} to response input stream body length", count);
					countThreadLocal.get().addAndGet(count);
				}
			}
		}

	}

	/**
	 * Get the thread-local length tracker.
	 * 
	 * @return the length tracker
	 */
	public final ThreadLocal<AtomicLong> countThreadLocal() {
		return countThreadLocal;
	}

}
