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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatServiceExportContext;
import net.solarnetwork.io.DeleteOnCloseFileResource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.JodaDatePropertySerializer;
import net.solarnetwork.util.ProgressListener;
import net.solarnetwork.util.PropertySerializer;

/**
 * Comma-separated-values implementation of
 * {@link DatumExportOutputFormatService}
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class CsvDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	private static final Set<String> CSV_CORE_HEADERS = Collections.unmodifiableSet(new LinkedHashSet<>(
			Arrays.asList("created", "nodeId", "sourceId", "localDate", "localTime")));

	private static final PropertySerializer JODA_DATE_TIME_PROP_SERIALIZER = new JodaDatePropertySerializer(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"));

	private static final CellProcessor JODA_DATE_TIME_CELL_PROCESSOR = new PropertySerializerCellProcessor(
			JODA_DATE_TIME_PROP_SERIALIZER);

	private static final PropertySerializer JODA_DATE_PROP_SERIALIZER = new JodaDatePropertySerializer(
			"yyyy-MM-dd", TimeZone.getTimeZone("UTC"));

	private static final CellProcessor JODA_DATE_CELL_PROCESSOR = new PropertySerializerCellProcessor(
			JODA_DATE_PROP_SERIALIZER);

	private static final PropertySerializer JODA_TIME_PROP_SERIALIZER = new JodaDatePropertySerializer(
			"HH:mm:ss.SSS", TimeZone.getTimeZone("UTC"));

	private static final CellProcessor JODA_TIME_CELL_PROCESSOR = new PropertySerializerCellProcessor(
			JODA_TIME_PROP_SERIALIZER);

	private static final CellProcessor DATE_TIME_CELL_PROCESSOR = new FmtDate(
			"yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

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
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicToggleSettingSpecifier("includeHeader", Boolean.TRUE));
		return result;
	}

	@Override
	public ExportContext createExportContext(OutputConfiguration config) {
		return new CsvExportContext(config, false); // TODO: make setting for including header
	}

	private class CsvExportContext extends BaseDatumExportOutputFormatServiceExportContext {

		private final CsvOutputFormatProperties props;
		private File temporaryFile;
		private ICsvMapWriter writer;
		private Set<String> headerSet;
		private String[] headers;
		private CellProcessor[] cellProcessors;

		private CsvExportContext(OutputConfiguration config, boolean includeHeader) {
			super(config);

			CsvOutputFormatProperties props = new CsvOutputFormatProperties();
			ClassUtils.setBeanProperties(props, config.getServiceProperties(), true);
			if ( !props.isValid() ) {
				throw new RuntimeException("CSV output service configuration is not valid.");
			}

			this.props = props;
		}

		private String[] headersForDatumMap(Map<String, Object> map) {
			String[] headers = new String[Math.max(CSV_CORE_HEADERS.size(), map.size())];
			int idx = 0;
			for ( String key : CSV_CORE_HEADERS ) {
				headers[idx++] = key;
			}
			for ( String key : map.keySet() ) {
				if ( CSV_CORE_HEADERS.contains(key) ) {
					continue;
				}
				headers[idx++] = key;
			}
			return headers;
		}

		private CellProcessor processorForDatumProperty(String key, Object value) {
			if ( value instanceof ReadableInstant ) {
				return JODA_DATE_TIME_CELL_PROCESSOR;
			} else if ( value instanceof LocalDate ) {
				return JODA_DATE_CELL_PROCESSOR;
			} else if ( value instanceof LocalTime ) {
				return JODA_TIME_CELL_PROCESSOR;
			} else if ( value instanceof Date ) {
				return DATE_TIME_CELL_PROCESSOR;
			}
			return null;
		}

		private CellProcessor[] processorsForDatumMap(Map<String, Object> map) {
			CellProcessor[] processors = new CellProcessor[Math.max(CSV_CORE_HEADERS.size(),
					map.size())];
			processors[0] = JODA_DATE_TIME_CELL_PROCESSOR;
			processors[1] = null;
			processors[2] = null;
			processors[3] = JODA_DATE_CELL_PROCESSOR;
			processors[4] = JODA_TIME_CELL_PROCESSOR;
			int idx = 5;
			for ( Map.Entry<String, ?> me : map.entrySet() ) {
				if ( CSV_CORE_HEADERS.contains(me.getKey()) ) {
					continue;
				}
				CellProcessor proc = processorForDatumProperty(me.getKey(), me.getValue());
				processors[idx++] = (proc != null ? new Optional(proc) : null);
			}
			return processors;
		}

		@SuppressWarnings("deprecation")
		private Map<String, Object> datumMap(GeneralNodeDatumFilterMatch match) {
			if ( match == null || match.getId() == null ) {
				return Collections.emptyMap();
			}
			Map<String, Object> map = new LinkedHashMap<String, Object>(8);

			map.put("created", match.getId().getCreated());
			map.put("nodeId", match.getId().getNodeId());
			map.put("sourceId", match.getId().getSourceId());
			map.put("localDate", match.getLocalDate());
			map.put("localTime", match.getLocalTime());

			Map<String, ?> sampleData = match.getSampleData();
			if ( sampleData != null ) {
				// don't allow overwrite of core properties (nodeId, etc)
				for ( Map.Entry<String, ?> me : sampleData.entrySet() ) {
					String key = me.getKey();
					if ( !map.containsKey(key) ) {
						map.put(key, me.getValue());
					}
				}
			}

			return map;
		}

		private boolean isSinglePassOutput() {
			return (!props.isIncludeHeader() || config == null || config.getCompressionType() == null);
		}

		@Override
		public void start(long estimatedResultCount) throws IOException {
			temporaryFile = createTemporaryResource(config);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(temporaryFile));
			if ( isSinglePassOutput() ) {
				// compress stream in single pass
				out = createCompressedOutputStream(out);
			}
			writer = new CsvMapWriter(new OutputStreamWriter(out, "UTF-8"),
					CsvPreference.STANDARD_PREFERENCE);
			setEstimatedResultCount(estimatedResultCount);
		}

		@Override
		public void appendDatumMatch(Iterable<? extends GeneralNodeDatumFilterMatch> iterable,
				ProgressListener<DatumExportService> progressListener) throws IOException {
			if ( writer == null ) {
				throw new UnsupportedOperationException("The start method must be called first.");
			}
			for ( GeneralNodeDatumFilterMatch m : iterable ) {
				Map<String, Object> map = datumMap(m);
				if ( map != null && !map.isEmpty() ) {
					if ( writer.getLineNumber() == 0 ) {
						headers = headersForDatumMap(map);
						headerSet = new LinkedHashSet<>(Arrays.asList(headers));
						if ( props.isIncludeHeader() && isSinglePassOutput() ) {
							writer.writeHeader(headers);
						}
						cellProcessors = processorsForDatumMap(map);
					} else {
						// check for column additions, and adjust header/processor structures accordingly
						for ( Map.Entry<String, Object> me : map.entrySet() ) {
							if ( !headerSet.contains(me.getKey()) ) {
								// new column!
								addColumnForMapProperty(me.getKey(), me.getValue());
							}
						}
					}
					writer.write(map, headers, cellProcessors);
				}
				incrementProgress(CsvDatumExportOutputFormatService.this, 1, progressListener);
			}
		}

		private void addColumnForMapProperty(String key, Object value) {
			String[] newHeaders = new String[headers.length + 1];
			System.arraycopy(headers, 0, newHeaders, 0, headers.length);
			newHeaders[headers.length] = key;
			headers = newHeaders;
			headerSet.add(key);
			CellProcessor[] newProcessors = new CellProcessor[cellProcessors.length + 1];
			System.arraycopy(cellProcessors, 0, newProcessors, 0, cellProcessors.length);
			newProcessors[cellProcessors.length] = processorForDatumProperty(key, value);
			cellProcessors = newProcessors;
		}

		@Override
		public Iterable<DatumExportResource> finish() throws IOException {
			flush();
			close();
			if ( !isSinglePassOutput() ) {
				// we have to concatenate headers + content
				File temporaryConcatenatedFile = createTemporaryResource(config);
				try (OutputStream out = createCompressedOutputStream(
						new BufferedOutputStream(new FileOutputStream(temporaryConcatenatedFile)));
						ICsvMapWriter concatenatedWriter = new CsvMapWriter(
								new OutputStreamWriter(StreamUtils.nonClosing(out), "UTF-8"),
								CsvPreference.STANDARD_PREFERENCE)) {
					concatenatedWriter.writeHeader(headers);
					concatenatedWriter.flush();
					FileCopyUtils.copy(new FileInputStream(temporaryFile), out);
				}
				temporaryFile.delete();
				temporaryFile = temporaryConcatenatedFile;
			}
			if ( temporaryFile != null ) {
				return Collections.singleton(new BasicDatumExportResource(
						new DeleteOnCloseFileResource(new FileSystemResource(temporaryFile)),
						getContentType(config)));
			}
			return Collections.emptyList();
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
