/* ==================================================================
 * BasicCsvDatumImportInputFormatService.java - 7/11/2018 2:24:40 PM
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

package net.solarnetwork.central.datum.imp.standard;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatServiceImportContext;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.ProgressListener;

/**
 * CSV import format that requires JSON formatted columns for the instantaneous,
 * accumulating, status, and tag sample properties of datum.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCsvDatumImportInputFormatService extends CsvDatumImportInputFormatServiceSupport {

	/**
	 * Constructor.
	 */
	public BasicCsvDatumImportInputFormatService() {
		super("net.solarnetwork.central.datum.imp.standard.BasicCsvDatumImportInputFormatService");
	}

	@Override
	public ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
			ProgressListener<DatumImportService> progressListener) throws IOException {
		if ( config == null ) {
			throw new IllegalArgumentException("No configuration provided.");
		}
		if ( resource == null ) {
			throw new IllegalArgumentException("No import resource provided.");
		}
		return new CsvImportContext(config, resource, progressListener);
	}

	private class CsvImportContext extends BaseDatumImportInputFormatServiceImportContext {

		private final BasicCsvDatumImportInputProperties props;

		/**
		 * Constructor.
		 * 
		 * @param config
		 *        the configuration
		 * @param resource
		 *        the data to import
		 */
		public CsvImportContext(InputConfiguration config, DatumImportResource resource,
				ProgressListener<DatumImportService> progressListener) throws IOException {
			super(config, resource, progressListener);
			BasicCsvDatumImportInputProperties props = new BasicCsvDatumImportInputProperties();
			ClassUtils.setBeanProperties(props, config.getServiceProperties(), true);
			if ( config.getTimeZoneId() != null ) {
				props.setTimeZoneId(config.getTimeZoneId());
			}
			if ( !props.isValid() ) {
				throw new IllegalArgumentException("Service configuration is not valid.");
			}
			this.props = props;
		}

		@Override
		public Iterator<GeneralNodeDatum> iterator() {
			try {
				return new CsvIterator(new CsvListReader(new InputStreamReader(
						getResourceProgressInputStream(BasicCsvDatumImportInputFormatService.this),
						getResourceCharset()), CsvPreference.STANDARD_PREFERENCE), props);
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}

		private class CsvIterator
				extends BaseCsvIterator<GeneralNodeDatum, BasicCsvDatumImportInputProperties> {

			private CsvIterator(ICsvListReader reader, BasicCsvDatumImportInputProperties props)
					throws IOException {
				super(reader, props);
			}

			@Override
			protected GeneralNodeDatum parseRow(List<String> row) throws IOException {
				GeneralNodeDatum d = parseDatum(row);

				GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();

				s.setInstantaneous(parseNumberMap(row, props.getInstantaneousDataColumn()));
				s.setAccumulating(parseNumberMap(row, props.getAccumulatingDataColumn()));
				s.setStatus(parseMap(row, props.getStatusDataColumn()));
				s.setTags(parseSet(row, props.getTagDataColumn()));

				if ( s.getInstantaneous() != null || s.getAccumulating() != null
						|| s.getStatus() != null ) {
					d.setSamples(s);
				}

				return d;
			}

		}

	}

	@Override
	public String getDisplayName() {
		return "Basic CSV";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return BasicCsvDatumImportInputProperties.getBasicCsvSettingSpecifiers();
	}

}
