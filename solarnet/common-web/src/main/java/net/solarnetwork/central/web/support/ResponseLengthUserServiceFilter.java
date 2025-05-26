/* ==================================================================
 * ResponseLengthUserServiceFilter.java - 5/05/2025 3:07:33 pm
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

package net.solarnetwork.central.web.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * Audit the response body length as a user service.
 *
 * @author matt
 * @version 1.0
 */
public final class ResponseLengthUserServiceFilter extends OncePerRequestFilter implements Filter {

	private final UserServiceAuditor userServiceAuditor;
	private final String auditServiceName;

	private Pattern[] excludes;

	private static final Logger log = LoggerFactory.getLogger(ResponseLengthUserServiceFilter.class);

	/**
	 * Constructor.
	 *
	 * @param userServiceAuditor
	 *        the auditor service to use
	 * @param auditServiceName
	 *        the audit service name to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ResponseLengthUserServiceFilter(UserServiceAuditor userServiceAuditor,
			String auditServiceName) {
		super();
		this.userServiceAuditor = requireNonNullArgument(userServiceAuditor, "userServiceAuditor");
		this.auditServiceName = requireNonNullArgument(auditServiceName, "auditServiceName");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		if ( excludes != null && excludes.length > 0 ) {
			String path = request.getRequestURI();
			if ( path != null ) {
				String ctxPath = request.getContextPath();
				if ( ctxPath != null && !ctxPath.isEmpty() ) {
					path = path.substring(ctxPath.length());
				}
				for ( Pattern p : excludes ) {
					if ( p.matcher(path).find() ) {
						log.trace("Excluding path [{}] because of matching pattern [{}]", path, p);
						chain.doFilter(request, response);
						return;
					}
				}
			}
		}

		chain.doFilter(request, new ResponseLengthTrackingHttpServletResponse(request, response));
	}

	private final class ResponseLengthTrackingHttpServletResponse extends HttpServletResponseWrapper {

		private final HttpServletRequest request;
		private ServletOutputStream out;
		private PrintWriter writer;

		/**
		 * Constructor.
		 *
		 * @param response
		 *        the response to wrap
		 */
		public ResponseLengthTrackingHttpServletResponse(HttpServletRequest request,
				HttpServletResponse response) {
			super(response);
			this.request = requireNonNullArgument(request, "request");
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if ( out != null ) {
				return out;
			}
			ServletOutputStream o = new DelegatingServletOutputStream(super.getOutputStream());
			this.out = o;
			return o;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if ( writer != null ) {
				return writer;
			}
			PrintWriter w = new DelegatingPrintWriter(super.getWriter());
			this.writer = w;
			return w;
		}

		private final class DelegatingServletOutputStream extends ServletOutputStream {

			private final ServletOutputStream delegate;
			private final Long userId;
			private long count = 0;

			private DelegatingServletOutputStream(ServletOutputStream delegate) {
				super();
				this.delegate = requireNonNullArgument(delegate, "delegate");
				Long userId = null;
				try {
					userId = SecurityUtils.getCurrentActorUserId();
				} catch ( BasicSecurityException e ) {
					log.debug("User ID not available!");
				}
				this.userId = userId;
			}

			@Override
			public void write(int b) throws IOException {
				if ( userId != null ) {
					count++;
				}
				delegate.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				if ( userId != null ) {
					count += len;
				}
				delegate.write(b, off, len);
			}

			@Override
			public boolean isReady() {
				return delegate.isReady();
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
				delegate.setWriteListener(writeListener);
			}

			@Override
			public boolean equals(Object obj) {
				return delegate.equals(obj);
			}

			@Override
			public int hashCode() {
				return delegate.hashCode();
			}

			@Override
			public void flush() throws IOException {
				if ( userId != null && count > 0 ) {
					log.debug("HTTP response to [{}] adds {} to user {} service {}",
							request.getRequestURI(), count, userId, auditServiceName);
					userServiceAuditor.auditUserService(userId, auditServiceName, (int) count);
					count = 0;
				}
				delegate.flush();
			}

			@Override
			public void close() throws IOException {
				if ( userId != null && count > 0 ) {
					log.debug("HTTP response to [{}] adds {} to user {} service {}",
							request.getRequestURI(), count, userId, auditServiceName);
					userServiceAuditor.auditUserService(userId, auditServiceName, (int) count);
					count = 0;
				}
				delegate.close();
			}

			@Override
			public String toString() {
				return delegate.toString();
			}

		}

		private final class DelegatingPrintWriter extends PrintWriter {

			private final Long userId;
			private long count = 0;

			private DelegatingPrintWriter(PrintWriter delegate) {
				super(delegate);
				Long userId = null;
				try {
					userId = SecurityUtils.getCurrentActorUserId();
				} catch ( BasicSecurityException e ) {
					log.debug("User ID not available!");
				}
				this.userId = userId;
			}

			@Override
			public void write(int c) {
				if ( userId != null ) {
					count++;
				}
				super.write(c);
			}

			@Override
			public void write(char[] buf, int off, int len) {
				if ( userId != null ) {
					count += len;
				}
				super.write(buf, off, len);
			}

			@Override
			public void write(String s, int off, int len) {
				if ( userId != null ) {
					count += len;
				}
				super.write(s, off, len);
			}

			@Override
			public void println() {
				if ( userId != null ) {
					count += System.lineSeparator().length();
				}
				super.println();
			}

			@Override
			public void flush() {
				if ( userId != null && count > 0 ) {
					log.debug("HTTP response to [{}] adds {} to user {} service {}",
							request.getRequestURI(), count, userId, auditServiceName);
					userServiceAuditor.auditUserService(userId, auditServiceName, (int) count);
					count = 0;
				}
				super.flush();
			}

			@Override
			public void close() {
				if ( userId != null && count > 0 ) {
					log.debug("HTTP response to [{}] adds {} to user {} service {}",
							request.getRequestURI(), count, userId, auditServiceName);
					userServiceAuditor.auditUserService(userId, auditServiceName, (int) count);
					count = 0;
				}
				super.close();
			}

		}

	}

	/**
	 * Get the optional URL path exclude patterns.
	 *
	 * @return the exclude patterns
	 */
	public Pattern[] getExcludes() {
		return excludes;
	}

	/**
	 * Set the optional URL path exclude patterns.
	 *
	 * @param excludes
	 *        the exclude patterns to set
	 */
	public void setExcludes(Pattern[] excludes) {
		this.excludes = excludes;
	}

}
