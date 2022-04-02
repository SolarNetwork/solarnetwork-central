/* ==================================================================
 * SimpleCsvDatumImportInputFormatService.java - 2/04/2022 4:37:58 PM
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

package net.solarnetwork.central.datum.imp.standard;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportInputFormatServiceImportContext;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * CSV import format that supports arbitrary columns for the instantaneous,
 * accumulating, status, and tag sample properties of datum by way of
 * classification mapping settings.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCsvDatumImportInputFormatService extends CsvDatumImportInputFormatServiceSupport {

	/**
	 * Constructor.
	 */
	public SimpleCsvDatumImportInputFormatService() {
		super("net.solarnetwork.central.datum.imp.standard.SimpleCsvDatumImportInputFormatService");
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

	private static Map<String, Number> parseNumberColumns(List<String> headerRow, List<String> row,
			IntRangeSet columns) {
		if ( columns == null ) {
			return null;
		}
		Map<String, Number> result = new LinkedHashMap<>(4);
		for ( Iterator<Integer> itr = columns.iterator(); itr.hasNext(); ) {
			final int i = itr.next() - 1;
			String val = (i < row.size() ? row.get(i) : null);
			Number n = StringUtils.numberValue(val);
			if ( n instanceof BigInteger ) {
				n = NumberUtils.narrow((BigInteger) n, 2);
			}
			if ( n != null ) {
				result.put(headerRow.get(i), n);
			}
		}
		return (result.isEmpty() ? null : result);
	}

	private static Map<String, Object> parseObjectColumns(List<String> headerRow, List<String> row,
			IntRangeSet columns) {
		if ( columns == null ) {
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(4);
		for ( Iterator<Integer> itr = columns.iterator(); itr.hasNext(); ) {
			final int i = itr.next();
			String val = (i < row.size() ? row.get(i) : null);
			Number n = StringUtils.numberValue(val);
			if ( n != null || val != null ) {
				result.put(headerRow.get(i), n != null ? n : val);
			}
		}
		return (result.isEmpty() ? null : result);
	}

	private static Set<String> parseSetColumns(List<String> headerRow, List<String> row,
			IntRangeSet columns) {
		if ( columns == null ) {
			return null;
		}
		Set<String> result = new LinkedHashSet<>(4);
		for ( Iterator<Integer> itr = columns.iterator(); itr.hasNext(); ) {
			final int i = itr.next();
			String val = (i < row.size() ? row.get(i) : null);
			if ( val != null ) {
				String[] tags = val.trim().split("\\s*,\\s*");
				for ( String tag : tags ) {
					result.add(tag);
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

	private class CsvImportContext extends BaseDatumImportInputFormatServiceImportContext {

		private final SimpleCsvDatumImportInputProperties props;
		private IntRangeSet instantaneousColumns;
		private IntRangeSet accumulatingColumns;
		private IntRangeSet statusColumns;
		private IntRangeSet tagColumns;

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
			SimpleCsvDatumImportInputProperties props = new SimpleCsvDatumImportInputProperties();
			ClassUtils.setBeanProperties(props, config.getServiceProperties(), true);
			if ( config.getTimeZoneId() != null ) {
				props.setTimeZoneId(config.getTimeZoneId());
			}
			if ( !props.isValid() ) {
				throw new IllegalArgumentException("Service configuration is not valid.");
			}
			this.props = props;
			this.instantaneousColumns = props.columnsForType(DatumSamplesType.Instantaneous);
			this.accumulatingColumns = props.columnsForType(DatumSamplesType.Accumulating);
			this.statusColumns = props.columnsForType(DatumSamplesType.Status);
			this.tagColumns = props.columnsForType(DatumSamplesType.Tag);
		}

		@Override
		public Iterator<GeneralNodeDatum> iterator() {
			try {
				return new CsvIterator(new CsvListReader(new InputStreamReader(
						getResourceProgressInputStream(SimpleCsvDatumImportInputFormatService.this),
						getResourceCharset()), CsvPreference.STANDARD_PREFERENCE), props);
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}

		private class CsvIterator
				extends BaseCsvIterator<GeneralNodeDatum, SimpleCsvDatumImportInputProperties> {

			private CsvIterator(ICsvListReader reader, SimpleCsvDatumImportInputProperties props)
					throws IOException {
				super(reader, props);
			}

			@Override
			protected GeneralNodeDatum parseRow(List<String> row) throws IOException {
				GeneralNodeDatum d = parseDatum(row);

				DatumSamples s = new DatumSamples();

				s.setInstantaneous(parseNumberColumns(headerRow, row, instantaneousColumns));
				s.setAccumulating(parseNumberColumns(headerRow, row, accumulatingColumns));
				s.setStatus(parseObjectColumns(headerRow, row, statusColumns));
				s.setTags(parseSetColumns(headerRow, row, tagColumns));

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
		return "Simple CSV";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SimpleCsvDatumImportInputProperties.getSimpleCsvSettingSpecifiers();
	}
}
