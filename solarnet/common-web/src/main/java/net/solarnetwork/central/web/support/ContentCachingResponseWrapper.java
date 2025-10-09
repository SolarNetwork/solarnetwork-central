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
import org.springframework.http.HttpHeaders;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * {@link jakarta.servlet.http.HttpServletResponse} wrapper that caches all
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
 * @version 1.5
 * @since 1.2
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

	private final FastByteArrayOutputStream content;
	private final OutputStream cacheStream;
	private final HttpHeaders headers;
	private ServletOutputStream outputStream;
	private PrintWriter outputWriter;
	private boolean cacheStreamFinished;
	private IOException outputStreamException;

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
	public void sendError(int sc, String msg) throws IOException {
		try {
			super.sendError(sc, msg);
		} catch ( IllegalStateException ex ) {
			// Possibly on Tomcat when called too late: fall back to silent setStatus
			super.setStatus(sc);
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
		resetContent();
		super.resetBuffer();
	}

	@Override
	public void reset() {
		resetContent();
		super.reset();
	}

	private void resetContent() {
		try {
			finishContentStream();
		} catch ( IOException e ) {
			// ignore
		}
		this.content.reset();
	}

	private void finishContentStream() throws IOException {
		if ( !cacheStreamFinished && cacheStream != content && outputStreamException == null ) {
			try {
				cacheStream.flush();
				cacheStream.close();
			} catch ( IOException e ) {
				// ignore
			}
			cacheStreamFinished = true;
		}
		if ( outputStreamException != null ) {
			throw outputStreamException;
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
		if ( outputStreamException == null ) {
			cacheStream.flush();
		}
		return content.size();
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

		private ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			try {
				if ( !cacheStreamFinished && outputStreamException == null ) {
					cacheStream.write(b);
				}
				this.os.write(b);
			} catch ( IOException e ) {
				if ( outputStreamException == null ) {
					outputStreamException = e;
				}
				throw e;
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			try {
				if ( !cacheStreamFinished && outputStreamException == null ) {
					cacheStream.write(b, off, len);
				}
				this.os.write(b, off, len);
			} catch ( IOException e ) {
				if ( outputStreamException == null ) {
					outputStreamException = e;
				}
				throw e;
			}
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
			try {
				if ( !cacheStreamFinished && outputStreamException == null ) {
					cacheStream.flush();
				}
				this.os.flush();
			} catch ( IOException e ) {
				if ( outputStreamException == null ) {
					outputStreamException = e;
				}
				throw e;
			}
		}

		@Override
		public void close() throws IOException {
			try {
				finishContentStream();
				this.os.close();
			} catch ( IOException e ) {
				if ( outputStreamException == null ) {
					outputStreamException = e;
				}
				throw e;
			}
		}

	}

	private static class ResponsePrintWriter extends PrintWriter {

		private ResponsePrintWriter(String characterEncoding, ServletOutputStream os)
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
