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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.support.BaseDatumExportOutputFormatServiceExportContext;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.codec.TemporalPropertySerializer;
import net.solarnetwork.io.DecompressingResource;
import net.solarnetwork.io.DeleteOnCloseFileResource;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ClassUtils;

/**
 * Comma-separated-values implementation of
 * {@link DatumExportOutputFormatService}
 *
 * @author matt
 * @version 2.3
 * @since 1.23
 */
public class CsvDatumExportOutputFormatService extends BaseDatumExportOutputFormatService {

	private static final Set<String> CSV_CORE_HEADERS = Collections.unmodifiableSet(new LinkedHashSet<>(
			Arrays.asList("created", "nodeId", "sourceId", "localDate", "localTime")));

	private static final PropertySerializer INSTANT_PROP_SERIALIZER = new TemporalPropertySerializer(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", ZoneOffset.UTC);

	private static final PropertySerializer DATE_PROP_SERIALIZER = new TemporalPropertySerializer(
			"yyyy-MM-dd", ZoneOffset.UTC);

	private static final PropertySerializer TIME_PROP_SERIALIZER = new TemporalPropertySerializer(
			"HH:mm:ss.SSS", ZoneOffset.UTC);

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

	public CsvDatumExportOutputFormatService() {
		super("net.solarnetwork.central.datum.export.standard.CsvDatumExportOutputFormatService");
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
		result.add(new BasicToggleSettingSpecifier("includeHeader", true));
		return result;
	}

	@Override
	public ExportContext createExportContext(OutputConfiguration config) {
		return new CsvExportContext(config);
	}

	private class CsvExportContext extends BaseDatumExportOutputFormatServiceExportContext {

		private final CsvOutputFormatProperties props;
		private File temporaryFile;
		private CsvWriter writer;
		private Set<String> headerSet;
		private String[] headers;
		private List<Function<Object, String>> cellProcessors;
		private int rowNum;

		private CsvExportContext(OutputConfiguration config) {
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

		private Function<Object, String> processorForDatumProperty(final Object value) {
			if ( value instanceof java.time.Instant || value instanceof ZonedDateTime
					|| value instanceof LocalDateTime ) {
				return (v) -> (String) INSTANT_PROP_SERIALIZER.serialize(null, null, v);
			} else if ( value instanceof LocalDate ) {
				return (v) -> (String) DATE_PROP_SERIALIZER.serialize(null, null, v);
			} else if ( value instanceof LocalTime ) {
				return (v) -> (String) TIME_PROP_SERIALIZER.serialize(null, null, v);
			} else if ( value instanceof Date ) {
				return (v) -> DATE_TIME_FORMATTER.format(((Date) v).toInstant());
			} else if ( value.getClass().isArray() ) {
				return (v) -> {
					String result = null;
					if ( v != null ) {
						if ( v.getClass().isArray() ) {
							result = StringUtils.arrayToCommaDelimitedString((Object[]) v);
						} else {
							result = v.toString();
						}
					}
					return result;
				};
			}
			return null;
		}

		private List<Function<Object, String>> processorsForDatumMap(Map<String, Object> map) {
			List<Function<Object, String>> processors = new ArrayList<>(
					Math.max(CSV_CORE_HEADERS.size(), map.size()));
			processors.add((value) -> (String) INSTANT_PROP_SERIALIZER.serialize(null, null, value));
			processors.add(null);
			processors.add(null);
			processors.add((value) -> (String) DATE_PROP_SERIALIZER.serialize(null, null, value));
			processors.add((value) -> (String) TIME_PROP_SERIALIZER.serialize(null, null, value));
			int idx = 5;
			for ( Map.Entry<String, ?> me : map.entrySet() ) {
				if ( CSV_CORE_HEADERS.contains(me.getKey()) ) {
					continue;
				}
				Function<Object, String> proc = processorForDatumProperty(me.getValue());
				processors.add(idx++, proc);
			}
			return processors;
		}

		@SuppressWarnings("MixedMutabilityReturnType")
		private Map<String, Object> datumMap(GeneralNodeDatumFilterMatch match) {
			if ( match == null || match.getId() == null ) {
				return Collections.emptyMap();
			}
			Map<String, Object> map = new LinkedHashMap<>(8);

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
			return !props.isIncludeHeader();
		}

		@Override
		public void start(long estimatedResultCount) throws IOException {
			temporaryFile = createTemporaryResource(config);
			BufferedOutputStream rawOut = new BufferedOutputStream(new FileOutputStream(temporaryFile));
			OutputStream out = rawOut;
			if ( isSinglePassOutput() ) {
				// compress stream in single pass
				out = createCompressedOutputStream(out);
			}
			if ( out == rawOut ) {
				// compress temporary results so don't run out of local disk space
				out = new GZIPOutputStream(out);
			}
			writer = CsvWriter.builder().build(out);
			setEstimatedResultCount(estimatedResultCount);
			log.info(
					"Starting CSV export with estimated row count {} to temporary file [{}] for config {}",
					estimatedResultCount, temporaryFile, config);
		}

		@Override
		public void appendDatumMatch(Iterable<? extends GeneralNodeDatumFilterMatch> iterable,
				ProgressListener<DatumExportService> progressListener) throws IOException {
			if ( writer == null ) {
				throw new UnsupportedOperationException("The start method must be called first.");
			}
			for ( GeneralNodeDatumFilterMatch m : iterable ) {
				Map<String, Object> map = datumMap(m);
				if ( !map.isEmpty() ) {
					if ( rowNum++ == 0 ) {
						headers = headersForDatumMap(map);
						headerSet = new LinkedHashSet<>(Arrays.asList(headers));
						if ( props.isIncludeHeader() && isSinglePassOutput() ) {
							writer.writeRecord(headers);
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
					String[] csvRow = new String[headers.length];
					for ( int i = 0; i < headers.length; i++ ) {
						Object val = map.get(headers[i]);
						if ( cellProcessors.get(i) != null ) {
							val = cellProcessors.get(i).apply(val);
						}
						if ( val != null ) {
							csvRow[i] = val.toString();
						}
					}
					writer.writeRecord(csvRow);
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
			cellProcessors.add(processorForDatumProperty(value));
		}

		@Override
		public Iterable<DatumExportResource> finish() throws IOException {
			flush();
			close();
			if ( temporaryFile == null ) {
				return Collections.emptyList();
			}
			log.info("Wrote {} bytes to temporary file [{}]", temporaryFile.length(), temporaryFile);
			final boolean decompressTemp = (config == null || config.getCompressionType() == null
					|| config.getCompressionType() == OutputCompressionType.None);
			if ( !isSinglePassOutput() ) {
				// we have to concatenate headers + content
				File temporaryConcatenatedFile = createTemporaryResource(config);
				// if the output does not have compression, we still keep the resource compressed locally to conserve disk space,
				// and just decompress the stream when sending to the output destination
				try (BufferedOutputStream rawOut = new BufferedOutputStream(
						new FileOutputStream(temporaryConcatenatedFile));
						OutputStream out = (decompressTemp ? new GZIPOutputStream(rawOut)
								: createCompressedOutputStream(rawOut))) {
					if ( headers != null ) {
						try (CsvWriter concatenatedWriter = CsvWriter.builder()
								.build(StreamUtils.nonClosing(out))) {
							concatenatedWriter.writeRecord(headers);
							concatenatedWriter.flush();
						}
					}
					FileCopyUtils.copy(new GZIPInputStream(new FileInputStream(temporaryFile)), out);
				}
				temporaryFile.delete();
				temporaryFile = temporaryConcatenatedFile;
				log.info("Wrote {} bytes to temporary file [{}]", temporaryFile.length(), temporaryFile);
			}
			if ( temporaryFile == null ) {
				return Collections.emptyList();
			}
			Resource outputResource = new FileSystemResource(temporaryFile);
			if ( decompressTemp ) {
				outputResource = new DecompressingResource(outputResource);
			}
			return Collections.singleton(
					new BasicDatumExportResource(new DeleteOnCloseFileResource(outputResource),
							getContentType(config), getExportContentType()));
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
				writer = null;
			}
		}

	}

}
