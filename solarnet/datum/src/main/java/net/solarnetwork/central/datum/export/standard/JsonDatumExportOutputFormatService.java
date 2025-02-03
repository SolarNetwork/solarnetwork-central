/* ==================================================================
 * JsonDatumExportOutputFormatService.java - 12/04/2018 6:53:40 AM
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

package net.solarnetwork.central.datum.export.standard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatServiceExportContext;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.io.DecompressingResource;
import net.solarnetwork.io.DeleteOnCloseFileResource;
import net.solarnetwork.service.ProgressListener;

/**
 * JSON implementation of {@link DatumExportOutputFormatService}
 *
 * @author matt
 * @version 2.1
 * @since 1.23
 */
public class JsonDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	private final ObjectMapper objectMapper;

	/**
	 * Default constructor.
	 */
	public JsonDatumExportOutputFormatService() {
		this(null);
	}

	/**
	 * Construct with an {@link ObjectMapper}.
	 *
	 * @param objectMapper
	 *        the object mapper to use, or {@code null} to create a standard one
	 */
	public JsonDatumExportOutputFormatService(ObjectMapper objectMapper) {
		super("net.solarnetwork.central.datum.export.standard.JsonDatumExportOutputFormatService");
		if ( objectMapper == null ) {
			objectMapper = JsonUtils.newObjectMapper();
		}
		this.objectMapper = objectMapper;
	}

	@Override
	public String getDisplayName() {
		return "JSON Output Format";
	}

	@Override
	public String getExportFilenameExtension() {
		return "json";
	}

	@Override
	public String getExportContentType() {
		return "application/json;charset=UTF-8";
	}

	@Override
	public ExportContext createExportContext(OutputConfiguration config) {
		return new JsonExportContext(config);
	}

	private class JsonExportContext extends BaseDatumExportOutputFormatServiceExportContext {

		private File temporaryFile;
		private JsonGenerator generator;
		private boolean started;

		private JsonExportContext(OutputConfiguration config) {
			super(config);
		}

		@Override
		public void start(long estimatedResultCount) throws IOException {
			setEstimatedResultCount(estimatedResultCount);
			temporaryFile = createTemporaryResource(config);
			BufferedOutputStream rawOut = new BufferedOutputStream(new FileOutputStream(temporaryFile));
			OutputStream out = createCompressedOutputStream(rawOut);
			if ( out == rawOut ) {
				// compress temporary results so don't run out of local disk space
				out = new GZIPOutputStream(out);
			}
			generator = objectMapper.getFactory().createGenerator(out);
		}

		@Override
		public void appendDatumMatch(Iterable<? extends GeneralNodeDatumFilterMatch> iterable,
				ProgressListener<DatumExportService> progressListener) throws IOException {
			if ( !started ) {
				generator.writeStartArray();
				started = true;
			}
			for ( GeneralNodeDatumFilterMatch match : iterable ) {
				generator.writeObject(match);
				incrementProgress(JsonDatumExportOutputFormatService.this, 1, progressListener);
			}
		}

		@Override
		public Iterable<DatumExportResource> finish() throws IOException {
			if ( started && !generator.isClosed() ) {
				generator.writeEndArray();
			}
			flush();
			close();
			final boolean decompressTemp = (config == null || config.getCompressionType() == null
					|| config.getCompressionType() == OutputCompressionType.None);
			if ( temporaryFile != null ) {
				log.info("Wrote {} bytes to temporary file [{}]", temporaryFile.length(), temporaryFile);
				Resource outputResource = new FileSystemResource(temporaryFile);
				if ( decompressTemp ) {
					outputResource = new DecompressingResource(outputResource);
				}
				return Collections.singleton(
						new BasicDatumExportResource(new DeleteOnCloseFileResource(outputResource),
								getContentType(config), getExportContentType()));
			}
			return Collections.emptyList();
		}

		@Override
		public void flush() throws IOException {
			if ( generator != null ) {
				generator.flush();
			}
		}

		@Override
		public void close() throws IOException {
			if ( generator != null ) {
				generator.close();
			}
		}

	}
}
