/* ==================================================================
 * CsvDatumExportOutputFormatService.java - 11/04/2018 12:08:19 PM
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
import java.io.OutputStreamWriter;
import java.util.Collections;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatServiceExportContext;

/**
 * Comma-separated-values implementation of
 * {@link DatumExportOutputFormatService}
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class CsvDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	public CsvDatumExportOutputFormatService() {
		super("net.solarnetwork.central.datum.biz.impl.CsvDatumExportOutputFormatService");
	}

	@Override
	public String getDisplayName() {
		return "CSV Output Format";
	}

	@Override
	public String getExportFilenameExtension() {
		return "csv";
	}

	@Override
	public String getExportContentType() {
		return "text/csv;charset=UTF-8";
	}

	@Override
	public ExportContext createExportContext(OutputConfiguration config) {
		return new CsvExportContext(config, false); // TODO: make setting for including header
	}

	private class CsvExportContext extends BaseDatumExportOutputFormatServiceExportContext {

		private final boolean includeHeader;
		private File temporaryFile;
		private ICsvBeanWriter writer;

		private CsvExportContext(OutputConfiguration config, boolean includeHeader) {
			super(config);
			this.includeHeader = includeHeader;
		}

		@Override
		public void start() throws IOException {
			temporaryFile = createTemporaryResource();
			OutputStream out = createCompressedOutputStream(
					new BufferedOutputStream(new FileOutputStream(temporaryFile)));
			writer = new CsvBeanWriter(new OutputStreamWriter(out, "UTF-8"),
					CsvPreference.STANDARD_PREFERENCE);
		}

		@Override
		public void appendDatumMatch(Iterable<? extends GeneralNodeDatumFilterMatch> iterable)
				throws IOException {
			if ( writer.getLineNumber() == 1 && includeHeader ) {
				// TODO write header
			}
			// TODO Auto-generated method stub
		}

		@Override
		public Iterable<Resource> finish() throws IOException {
			flush();
			close();
			return Collections.singleton(new FileSystemResource(temporaryFile));
		}

		@Override
		public void flush() throws IOException {
			if ( writer != null ) {
				writer.flush();
			}
		}

		@Override
		public void close() throws IOException {
			if ( writer != null ) {
				writer.close();
			}
		}

	}

}
