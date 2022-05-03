/* ==================================================================
 * ContentCachingResponseWrapper.java - 3/05/2022 10:46:05 am
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

package net.solarnetwork.central.web.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.http.HttpServletResponse} wrapper that caches all
 * content written to the {@linkplain #getOutputStream() output stream}, and
 * allows this content to be retrieved via a {@link #getContentAsByteArray()
 * byte array}.
 * 
 * <p>
 * Note that the content is copied to the underlying servlet stream <em>as
 * well</em> as the cached content buffer. This wrapper supports compressing the
 * cached content with gzip.
 * </p>
 * 
 * <p>
 * Originally based on
 * {@link org.springframework.web.util.ContentCachingResponseWrapper}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	private final FastByteArrayOutputStream content;
	private final OutputStream cacheStream;
	private final HttpHeaders headers;
	private ServletOutputStream outputStream;
	private PrintWriter outputWriter;
	private boolean cacheStreamFinished;

	/**
	 * Create a new ContentCachingResponseWrapper for the given servlet
	 * response.
	 * 
	 * @param response
	 *        the original servlet response
	 * @param gzip
	 *        {@literal true} to compress the cached content with gzip
	 */
	public ContentCachingResponseWrapper(HttpServletResponse response, boolean gzip) throws IOException {
		super(response);
		this.content = new FastByteArrayOutputStream(1024);
		this.cacheStream = (gzip ? new GZIPOutputStream(this.content) : this.content);
		this.headers = new HttpHeaders();
	}

	@Override
	public void setHeader(String name, String value) {
		headers.set(name, value);
		super.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		headers.add(name, value);
		super.addHeader(name, value);
	}

	@Override
	public void sendError(int sc) throws IOException {
		try {
			super.sendError(sc);
		} catch ( IllegalStateException ex ) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void sendError(int sc, String msg) throws IOException {
		try {
			super.sendError(sc, msg);
		} catch ( IllegalStateException ex ) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc, msg);
		}
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if ( this.outputStream == null ) {
			this.outputStream = new ResponseServletOutputStream(getResponse().getOutputStream());
		}
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if ( this.outputWriter == null ) {
			String characterEncoding = getCharacterEncoding();
			this.outputWriter = new ResponsePrintWriter(
					characterEncoding != null ? characterEncoding : WebUtils.DEFAULT_CHARACTER_ENCODING,
					getOutputStream());
		}
		return this.outputWriter;
	}

	@Override
	public void setBufferSize(int size) {
		if ( size > this.content.size() ) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	private void finishContentStream() throws IOException {
		if ( !cacheStreamFinished && this.cacheStream != this.content ) {
			this.cacheStream.flush();
			this.cacheStream.close();
			cacheStreamFinished = true;
		}
	}

	/**
	 * Return the cached response content as a byte array.
	 */
	public byte[] getContentAsByteArray() throws IOException {
		finishContentStream();
		return this.content.toByteArray();
	}

	/**
	 * Return an {@link InputStream} to the cached content.
	 */
	public InputStream getContentInputStream() throws IOException {
		finishContentStream();
		return this.content.getInputStream();
	}

	/**
	 * Return the current size of the cached content.
	 */
	public int getContentSize() throws IOException {
		this.cacheStream.flush();
		return this.content.size();
	}

	/**
	 * Test if the cached content has been compressed with gzip.
	 * 
	 * @return {@literal true} if the cached content is compressed with gzip
	 */
	public boolean isContentGzip() {
		return (cacheStream instanceof GZIPOutputStream);
	}

	/**
	 * Get the HTTP headers.
	 * 
	 * @return the headers
	 */
	public HttpHeaders getHttpHeaders() {
		return headers;
	}

	private class ResponseServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream os;

		public ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			cacheStream.write(b);
			this.os.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			cacheStream.write(b, off, len);
			this.os.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return this.os.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.os.setWriteListener(writeListener);
		}

		@Override
		public void flush() throws IOException {
			cacheStream.flush();
			super.flush();
		}

		@Override
		public void close() throws IOException {
			finishContentStream();
			super.close();
		}

	}

	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding, ServletOutputStream os)
				throws UnsupportedEncodingException {
			super(new OutputStreamWriter(os, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}
