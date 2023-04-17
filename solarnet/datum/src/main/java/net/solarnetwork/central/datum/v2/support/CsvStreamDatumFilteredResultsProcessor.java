/* ==================================================================
 * CsvStreamDatumFilteredResultsProcessor.java - 17/04/2023 2:36:06 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.MimeType;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.support.CsvFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * {@link FilteredResultsProcessor} for encoding overall results into datum
 * stream CSV form.
 * 
 * <p>
 * This processor requires a {@link ObjectDatumStreamMetadataProvider} instance
 * be provided in the {@code attributes} argument on the
 * {@link #start(Long, Integer, Integer, Map)} method, on the
 * {@link StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR} key.
 * </p>
 * 
 * <h2>Non-aggregate column structure</h2>
 * 
 * <p>
 * The output CSV column structure for <b>non-aggregate</b> datum starts with:
 * </p>
 * 
 * <ol>
 * <li><b>ts</b> - timestamp</li>
 * <li><b>streamId</b> - stream ID</li>
 * <li><b>objectId</b> - node or location ID</li>
 * <li><b>sourceId</b> - source ID</li>
 * </ol>
 * 
 * <p>
 * Then columns are added for all instantaneous, accumulating, and status
 * properties of all available streams. Only unique property names are added as
 * additional columns. A final additional column is added for tags, which are
 * joined into a single comma-delimited string value.
 * </p>
 * 
 * <p>
 * For example, imagine there are two datum streams being processed, with stream
 * metadata like:
 * </p>
 * 
 * <pre>{@code [
 * {"i":["a", "b", "c"], "a":["d", "e"], "s":["f", "g"]},
 * {"i":["b", "h", "i"], "a":["c"]}
 * ]}</pre>
 * 
 * <p>
 * Notice how the {@code b} and {@code c} property names are used in both
 * streams. The resulting CSV column structure would be:
 * </p>
 * 
 * <pre>{@code
 * ts,streamId,objectId,sourceId,a,b,c,d,e,f,g,h,i,tags
 * }</pre>
 * 
 * <h2>Aggregate column structure</h2>
 * 
 * <p>
 * The output CSV column structure for <b>aggregate</b> datum starts with:
 * </p>
 * 
 * <ol>
 * <li><b>ts_start</b> - starting timestamp</li>
 * <li><b>ts_end</b> - ending timestamp, or empty if implied by the query</li>
 * <li><b>streamId</b> - stream ID</li>
 * <li><b>objectId</b> - node or location ID</li>
 * <li><b>sourceId</b> - source ID</li>
 * </ol>
 * 
 * <p>
 * Then columns are added for all instantaneous, accumulating, and status
 * properties of all available streams. For each <em>instantaneous</em> property
 * 3 additional statistic columns are added, where <b>{@code P}</b> is the
 * property name:
 * </p>
 * 
 * <ol>
 * <li><b>{@code P_count}</b> - the number of values within the aggregate
 * period</li>
 * <li><b>{@code P_min}</b> - the minimum value within the aggregate period</li>
 * <li><b>{@code P_max}</b> - the maximum value within the aggregate period</li>
 * </ol>
 * 
 * <p>
 * For each <em>accumulating</em> property 2 additional statistic columns are
 * added, where <b>{@code P}</b> is the property name:
 * </p>
 * 
 * <ol>
 * <li><b>{@code P_start}</b> - the value at the start of the aggregate
 * period</li>
 * <li><b>{@code P_end}</b> - the value at the end of the aggregate period</li>
 * </ol>
 * 
 * <p>
 * A final additional column is added for tags, which are joined into a single
 * comma-delimited string value.
 * </p>
 * 
 * <p>
 * For example, imagine there are two aggregate datum streams being processed,
 * with stream metadata like:
 * </p>
 * 
 * <pre>{@code [
 * {"i":["a", "b", "a":["c"], "s":["d"]},
 * {"i":["b"], "a":["e"]}
 * ]}</pre>
 * 
 * <p>
 * Notice how the {@code b} property name is used in both streams. The resulting
 * CSV column structure would be:
 * </p>
 * 
 * <pre>{@code
 * ts,streamId,objectId,sourceId,a,a_count,a_min,a_max,b,b_count,b_min,b_max,c,c_start,c_end,d,e,e_start,e_end,tags
 * }</pre>
 * 
 * @author matt
 * @version 1.0
 * @since 1.11
 */
public class CsvStreamDatumFilteredResultsProcessor implements StreamDatumFilteredResultsProcessor {

	/** The CSV media type. */
	public static final MimeType TEXT_CSV_MIME_TYPE = CsvFilteredResultsProcessor.TEXT_CSV_MIME_TYPE;

	/** The output destination. */
	private final ICsvListWriter writer;

	/**
	 * A mapping of column names to associated column index; linked so insertion
	 * order is column order.
	 */
	private final Map<String, Integer> streamColumnIndexes = new LinkedHashMap<>(8);

	private ObjectDatumStreamMetadataProvider metadataProvider;
	private Collection<UUID> streamIds;
	private int columnCount = 0;
	private int metaColumnCount = 0;

	/**
	 * Constructor.
	 * 
	 * @param out
	 *        the output destination
	 */
	public CsvStreamDatumFilteredResultsProcessor(Writer out) {
		super();
		this.writer = new CsvListWriter(requireNonNullArgument(out, "out"),
				CsvPreference.STANDARD_PREFERENCE);

	}

	@Override
	public MimeType getMimeType() {
		return TEXT_CSV_MIME_TYPE;
	}

	@Override
	public void start(Long totalResultCount, Integer startingOffset, Integer expectedResultCount,
			Map<String, ?> attributes) throws IOException {
		if ( attributes == null || !(attributes
				.get(METADATA_PROVIDER_ATTR) instanceof ObjectDatumStreamMetadataProvider) ) {
			throw new IllegalArgumentException("No metadata provider provided.");
		}
		metadataProvider = (ObjectDatumStreamMetadataProvider) attributes.get(METADATA_PROVIDER_ATTR);
	}

	private static final String COUNT_FORMAT = "%s_count";
	private static final String MIN_FORMAT = "%s_min";
	private static final String MAX_FORMAT = "%s_max";
	private static final String START_FORMAT = "%s_start";
	private static final String END_FORMAT = "%s_end";

	@Override
	public void handleResultItem(StreamDatum resultItem) throws IOException {
		if ( metadataProvider == null ) {
			return;
		}

		final boolean agg = (resultItem instanceof AggregateDatum);
		final boolean reading = (resultItem instanceof ReadingDatum);

		// if this is the first result, generate a CSV header row out of all available metadata
		if ( streamIds == null ) {
			streamIds = metadataProvider.metadataStreamIds();
			if ( streamIds == null ) {
				streamIds = Collections.emptySet();
			}
			if ( !streamIds.isEmpty() ) {
				int colIndex = 0;
				for ( UUID streamId : streamIds ) {
					ObjectDatumStreamMetadata meta = metadataProvider.metadataForStreamId(streamId);
					if ( meta != null ) {
						String[] propNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
						if ( propNames != null ) {
							for ( String propName : propNames ) {
								if ( streamColumnIndexes.putIfAbsent(propName, colIndex) == null ) {
									colIndex += 1;
									if ( agg ) {
										streamColumnIndexes.put(COUNT_FORMAT.formatted(propName),
												colIndex++);
										streamColumnIndexes.put(MIN_FORMAT.formatted(propName),
												colIndex++);
										streamColumnIndexes.put(MAX_FORMAT.formatted(propName),
												colIndex++);
									}
								}
							}
						}
						propNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
						if ( propNames != null ) {
							for ( String propName : propNames ) {
								if ( streamColumnIndexes.putIfAbsent(propName, colIndex) == null ) {
									colIndex += 1;
									if ( agg ) {
										streamColumnIndexes.put(START_FORMAT.formatted(propName),
												colIndex++);
										streamColumnIndexes.put(END_FORMAT.formatted(propName),
												colIndex++);
									}
								}
							}
						}
						propNames = meta.propertyNamesForType(DatumSamplesType.Status);
						if ( propNames != null ) {
							for ( String propName : propNames ) {
								if ( streamColumnIndexes.putIfAbsent(propName, colIndex) == null ) {
									colIndex += 1;
								}
							}
						}
					}
				}
				metaColumnCount = (agg ? 5 : 4);
				columnCount = metaColumnCount + streamColumnIndexes.keySet().size() + 1;
				String[] header = new String[columnCount];
				int i = 0;
				if ( agg ) {
					header[i++] = "ts_start";
					header[i++] = "ts_end";
				} else {
					header[i++] = "ts";
				}
				header[i++] = "streamId";
				header[i++] = "objectId";
				header[i++] = "sourceId";
				header[header.length - 1] = "tags";
				String[] propNames = streamColumnIndexes.keySet().stream().toArray(String[]::new);
				System.arraycopy(propNames, 0, header, metaColumnCount, propNames.length);
				writer.writeHeader(header);
			}
		}

		ObjectDatumStreamMetadata meta = metadataProvider.metadataForStreamId(resultItem.getStreamId());
		if ( meta == null ) {
			return;
		}

		String[] row = new String[columnCount];
		row[0] = formatInstant(resultItem.getTimestamp());
		if ( agg ) {
			row[1] = formatInstant(reading ? ((ReadingDatum) resultItem).getEndTimestamp() : null);
			row[2] = formatObject(resultItem.getStreamId());
			row[3] = formatObject(meta.getObjectId());
			row[4] = formatObject(meta.getSourceId());
		} else {
			row[1] = formatObject(resultItem.getStreamId());
			row[2] = formatObject(meta.getObjectId());
			row[3] = formatObject(meta.getSourceId());
		}
		Arrays.fill(row, metaColumnCount, row.length, "");

		populateRow(meta, resultItem, agg, DatumSamplesType.Instantaneous, row);
		populateRow(meta, resultItem, agg, DatumSamplesType.Accumulating, row);
		populateRow(meta, resultItem, agg, DatumSamplesType.Status, row);
		row[row.length - 1] = arrayToCommaDelimitedString(resultItem.getProperties().getTags());
		writer.write(row);
	}

	private void populateRow(ObjectDatumStreamMetadata meta, StreamDatum resultItem, final boolean agg,
			final DatumSamplesType type, final String[] row) {
		final DatumProperties props = resultItem.getProperties();
		final String[] propNames = meta.propertyNamesForType(type);
		if ( propNames != null ) {
			Object[] propVals = switch (type) {
				case Instantaneous -> props.getInstantaneous();
				case Accumulating -> props.getAccumulating();
				case Status -> props.getStatus();
				case Tag -> throw new IllegalArgumentException("Tag type not supported.");
			};
			if ( propVals == null ) {
				return;
			}
			for ( int i = 0, len = propVals.length; i < len; i++ ) {
				Integer columnIdx = streamColumnIndexes.get(propNames[i]);
				if ( columnIdx == null ) {
					continue;
				}
				int idx = metaColumnCount + columnIdx;
				if ( idx >= row.length ) {
					continue;
				}
				row[idx] = (type == DatumSamplesType.Status ? formatObject(propVals[i])
						: formatDecimal((BigDecimal) propVals[i]));

				if ( agg && type == DatumSamplesType.Instantaneous ) {
					DatumPropertiesStatistics stats = ((AggregateDatum) resultItem).getStatistics();
					row[idx + 1] = formatDecimal(stats.getInstantaneousCount(i));
					row[idx + 2] = formatDecimal(stats.getInstantaneousMinimum(i));
					row[idx + 3] = formatDecimal(stats.getInstantaneousMaximum(i));
				} else if ( agg && type == DatumSamplesType.Accumulating ) {
					DatumPropertiesStatistics stats = ((AggregateDatum) resultItem).getStatistics();
					row[idx + 1] = formatDecimal(stats.getAccumulatingStart(i));
					row[idx + 2] = formatDecimal(stats.getAccumulatingEnd(i));
				}
			}
		}
	}

	private static String formatInstant(Instant ts) {
		if ( ts == null ) {
			return "";
		}
		return DateTimeFormatter.ISO_INSTANT.format(ts);
	}

	private static String formatDecimal(BigDecimal obj) {
		if ( obj == null ) {
			return "";
		}
		return obj.toPlainString();
	}

	private static String formatObject(Object obj) {
		if ( obj == null ) {
			return "";
		}
		return obj.toString();
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
