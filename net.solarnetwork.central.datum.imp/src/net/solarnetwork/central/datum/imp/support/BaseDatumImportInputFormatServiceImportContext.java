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

import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.MimeType;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.util.ProgressListener;

/**
 * Base class for {@link ImportContext} implementations.
 * 
 * @author matt
 * @version 1.0
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
		return new DatumImportProgressInputStream(resource.getInputStream(), resource.contentLength(),
				context, progressListener);
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

	protected void setEstimatedResultCount(long estimatedResultCount) {
		this.estimatedResultCount = estimatedResultCount;
	}

	protected void incrementProgress(DatumImportService context, int count,
			ProgressListener<DatumImportService> progressListener) {
		if ( estimatedResultCount < 1 ) {
			return;
		}
		complete += count;
		progressListener.progressChanged(context,
				Math.min(1.0, (double) complete / (double) estimatedResultCount));
	}

	@Override
	public void close() throws IOException {
		// extending classes might do something
	}

}
