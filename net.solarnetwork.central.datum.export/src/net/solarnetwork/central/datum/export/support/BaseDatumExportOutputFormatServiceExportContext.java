/* ==================================================================
 * BaseDatumExportOutputFormatServiceExportContext.java - 22/04/2018 8:04:48 AM
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

package net.solarnetwork.central.datum.export.support;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService.ExportContext;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.util.ProgressListener;

/**
 * Base class for {@link ExportContext} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseDatumExportOutputFormatServiceExportContext implements ExportContext {

	protected final OutputConfiguration config;
	private long estimatedResultCount = -1;
	private long complete = 0;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration
	 */
	public BaseDatumExportOutputFormatServiceExportContext(OutputConfiguration config) {
		super();
		this.config = config;
	}

	protected void setEstimatedResultCount(long estimatedResultCount) {
		this.estimatedResultCount = estimatedResultCount;
	}

	protected void incrementProgress(DatumExportService context, int count,
			ProgressListener<DatumExportService> progressListener) {
		if ( estimatedResultCount < 1 ) {
			return;
		}
		complete += count;
		progressListener.progressChanged(context,
				Math.min(1.0, (double) complete / (double) estimatedResultCount));
	}

	/**
	 * Wrap an output stream with a compressed stream, if the configuration
	 * requests compression.
	 * 
	 * @param out
	 *        the output stream to wrap
	 * @return the output stream to use, possibly wrapped with a compression
	 *         stream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected OutputStream createCompressedOutputStream(OutputStream out) throws IOException {
		if ( config != null && config.getCompressionType() != null ) {
			switch (config.getCompressionType()) {
				case GZIP:
					out = new GZIPOutputStream(out);
					break;

				case XZ:
					try {
						out = new CompressorStreamFactory()
								.createCompressorOutputStream(CompressorStreamFactory.XZ, out);
					} catch ( CompressorException e ) {
						throw new IOException(e);
					}

				default:
					// nothing more
			}
		}
		return out;
	}

	@Override
	public void close() throws IOException {
		// extending classes might do something
	}

	@Override
	public void flush() throws IOException {
		// extending classes might do something
	}

}
