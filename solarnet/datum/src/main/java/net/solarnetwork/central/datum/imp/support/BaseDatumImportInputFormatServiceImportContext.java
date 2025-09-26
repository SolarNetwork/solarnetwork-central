/* ==================================================================
 * BaseDatumImportInputFormatServiceImportContext.java - 7/11/2018 9:07:34 PM
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

package net.solarnetwork.central.datum.imp.support;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.util.MimeType;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.service.ProgressListener;

/**
 * Base class for {@link ImportContext} implementations.
 *
 * @author matt
 * @version 2.1
 */
public abstract class BaseDatumImportInputFormatServiceImportContext implements ImportContext {

	protected final InputConfiguration config;
	protected final DatumImportResource resource;
	protected final ProgressListener<DatumImportService> progressListener;
	private long estimatedResultCount = -1;
	private long complete = 0;

	/**
	 * Constructor.
	 *
	 * @param config
	 *        the configuration
	 * @param resource
	 *        the data to import
	 * @param progressListener
	 *        the progress listener
	 */
	public BaseDatumImportInputFormatServiceImportContext(InputConfiguration config,
			DatumImportResource resource, ProgressListener<DatumImportService> progressListener) {
		super();
		this.config = config;
		this.resource = resource;
		this.progressListener = progressListener;
	}

	/**
	 * Get an {@link InputStream} that tracks the progress of reading the stream
	 * content against the reported length of the import resource.
	 *
	 * @param context
	 *        the progress context
	 * @return the input stream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected InputStream getResourceProgressInputStream(DatumImportService context) throws IOException {
		MutableLong byteCount = new MutableLong(0);
		InputStream in = BoundedInputStream.builder().setInputStream(resource.getInputStream())
				.setAfterRead(n -> {
					if ( n > 0 && progressListener != null ) {
						byteCount.add(n);
						progressListener.progressChanged(context,
								Math.min(1.0, byteCount.doubleValue() / resource.contentLength()));
					}
				}).get();

		// see if we can decompress
		BufferedInputStream bufIn = new BufferedInputStream(in);
		String compressionType = null;
		try {
			compressionType = CompressorStreamFactory.detect(bufIn);
		} catch ( CompressorException e ) {
			// ignore and treat as "not compressed"
		}
		if ( compressionType != null ) {
			try {
				return new CompressorStreamFactory().createCompressorInputStream(compressionType, bufIn);
			} catch ( CompressorException e ) {
				throw new IOException(
						"Error handling compression of resource " + resource + ": " + e.getMessage());
			}
		}
		return bufIn;
	}

	/**
	 * Get the character encoding of the resource.
	 *
	 * <p>
	 * This will default to {@literal UTF-8} if the encoding cannot otherwise be
	 * determined.
	 * </p>
	 *
	 * @return the character set name, never {@literal null}
	 */
	protected String getResourceCharset() {
		String charset = "UTF-8";
		if ( resource != null && resource.getContentType() != null ) {
			MimeType mime = MimeType.valueOf(resource.getContentType());
			String csParam = mime.getParameter("charset");
			if ( csParam != null ) {
				charset = csParam;
			}
		}
		return charset;
	}

	/**
	 * Get the completed count, against the estimated result count.
	 *
	 * @return the count
	 * @see #setEstimatedResultCount(long)
	 */
	protected long getCompleteCount() {
		return complete;
	}

	/**
	 * Get the estimated result count.
	 *
	 * @return the estimated result count
	 */
	protected long getEstimatedResultCount() {
		return estimatedResultCount;
	}

	/**
	 * Set the estimated result count.
	 *
	 * @param estimatedResultCount
	 *        the estimated result count
	 * @see #incrementProgress(DatumImportService, long, ProgressListener)
	 * @see #updateProgress(DatumImportService, long, ProgressListener)
	 */
	protected void setEstimatedResultCount(long estimatedResultCount) {
		this.estimatedResultCount = estimatedResultCount;
	}

	/**
	 * Increment progress against the estimated result count.
	 *
	 * <p>
	 * The {@code count} will be added to the current progress amount. Use
	 * {@link #updateProgress(DatumImportService, long, ProgressListener)} to
	 * set the absolute progress amount.
	 * </p>
	 *
	 * @param context
	 *        the context
	 * @param count
	 *        the progress amount to add
	 * @param progressListener
	 *        the progress listener
	 * @see #setEstimatedResultCount(long)
	 */
	protected void incrementProgress(DatumImportService context, long count,
			ProgressListener<DatumImportService> progressListener) {
		if ( estimatedResultCount < 1 ) {
			return;
		}
		complete += count;
		progressListener.progressChanged(context,
				Math.min(1.0, (double) complete / (double) estimatedResultCount));
	}

	/**
	 * Set the progress amount against the estimated result count.
	 *
	 * <p>
	 * The {@code amount} value is the absolute progress value to set. Use
	 * {@link #incrementProgress(DatumImportService, long, ProgressListener)} to
	 * add a count to the current amount.
	 * </p>
	 *
	 * @param context
	 *        the context
	 * @param amount
	 *        the progress amount to set
	 * @param progressListener
	 *        the progress listener
	 * @see #setEstimatedResultCount(long)
	 */
	protected void updateProgress(DatumImportService context, long amount,
			ProgressListener<DatumImportService> progressListener) {
		if ( estimatedResultCount < 1 ) {
			return;
		}
		complete = amount;
		progressListener.progressChanged(context,
				Math.min(1.0, (double) complete / (double) estimatedResultCount));
	}

	@Override
	public void close() throws IOException {
		// extending classes might do something
	}

}
