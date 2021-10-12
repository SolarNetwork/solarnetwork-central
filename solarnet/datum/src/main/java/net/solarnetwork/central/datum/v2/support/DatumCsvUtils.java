/* ==================================================================
 * DatumCsvUtils.java - 18/05/2021 3:12:40 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.CloseableIterator;
import net.solarnetwork.util.DateUtils;

/**
 * Utilities for Datum CSV processing.
 * 
 * @author matt
 * @version 2.0
 * @since 2.9
 */
public final class DatumCsvUtils {

	private DatumCsvUtils() {
		// don't construct me
	}

	/**
	 * A timestamp formatter like {@link DateUtils#ISO_DATE_OPT_TIME_ALT} but
	 * using an hour-only offset pattern.
	 */
	public static final DateTimeFormatter ISO_DATE_OPT_TIME_ALT_HOUR_OFFSET;
	static {
		// @formatter:off
		ISO_DATE_OPT_TIME_ALT_HOUR_OFFSET = new DateTimeFormatterBuilder()
				.append(DateTimeFormatter.ISO_DATE)
				.optionalStart()
				.appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .optionalStart()
                .appendOffset("+HH", "+00")
				.toFormatter();
		// @formatter:on
	}

	/**
	 * Parse CSV formatted stream metadata.
	 * 
	 * @param in
	 *        the input to parse as CSV
	 * @param kind
	 *        the kind to treat the results as
	 * @param zone
	 *        a time zone to use, if the data does not include one
	 * @return the list of metadata, never {@literal null}
	 * @throws IOException
	 *         if any parsing error occurs
	 */
	public static List<ObjectDatumStreamMetadata> parseMetadata(Reader in, ObjectDatumKind kind,
			ZoneId zone) throws IOException {
		List<ObjectDatumStreamMetadata> result = new ArrayList<>();
		try (ICsvListReader r = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			r.getHeader(true);
			List<String> row = null;
			while ( (row = r.read()) != null ) {
				if ( row.size() < 8 ) {
					continue;
				}
				final UUID streamId = UUID.fromString(row.get(0));
				final Long objId = Long.valueOf(row.get(1));
				final String sourceId = row.get(2);
				// we skip created/updated columns 3/4

				String[] iCol = parseArrayValue(row.get(5));
				String[] aCol = parseArrayValue(row.get(6));
				String[] sCol = parseArrayValue(row.get(7));

				String jMeta = null;
				if ( row.size() > 8 ) {
					jMeta = row.get(8);
				}

				ObjectDatumKind k;
				if ( row.size() > 9 ) {
					k = ObjectDatumKind.forKey(row.get(9));
				} else {
					k = kind;
				}

				String tz;
				if ( row.size() > 10 ) {
					tz = row.get(10);
				} else {
					tz = zone.getId();
				}

				BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, tz, k,
						objId, sourceId, null, iCol, aCol, sCol, jMeta);
				result.add(meta);
			}
		}
		return result;
	}

	private static String[] parseArrayValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		if ( value.startsWith("{") ) {
			value = value.substring(1);
		}
		if ( value.endsWith("}") ) {
			value = value.substring(0, value.length() - 1);
		}
		return commaDelimitedListToStringArray(value);
	}

	private static String[][] parse2dArrayValue(String value) {
		if ( value.startsWith("{") ) {
			value = value.substring(1);
		}
		if ( value.endsWith("}") ) {
			value = value.substring(0, value.length() - 1);
		}
		String[] components = value.split("\\}\\s*,\\s*\\{");
		String[][] result = new String[components.length][];
		for ( int i = 0; i < components.length; i++ ) {
			result[i] = parseArrayValue(components[i]);
		}
		return result;
	}

	private static BigDecimal[][] parase2dDecimalArray(String[][] strings) {
		if ( strings == null ) {
			return null;
		}
		if ( strings.length == 0 ) {
			return new BigDecimal[0][];
		}
		BigDecimal[][] result = new BigDecimal[strings.length][];
		for ( int i = 0; i < strings.length; i++ ) {
			result[i] = decimalArray(strings[i]);
		}
		return result;
	}

	private static Instant parseInstant(String value) {
		try {
			return ISO_DATE_OPT_TIME_ALT_HOUR_OFFSET.parse(value, Instant::from);
		} catch ( DateTimeParseException e ) {
			try {
				return DateUtils.ISO_DATE_OPT_TIME_ALT.parse(value, Instant::from);
			} catch ( DateTimeParseException e2 ) {
				return DateTimeFormatter.ISO_INSTANT.parse(value, Instant::from);
			}
		}
	}

	/**
	 * Parse CSV formatted aggregate datum.
	 * 
	 * @param in
	 *        the input to parse as CSV
	 * @param aggregation
	 *        the aggregate type
	 * @return the list of aggregate datum, never {@literal null}
	 * @throws IOException
	 *         if any parsing error occurs
	 */
	public static List<AggregateDatum> parseAggregateDatum(Reader in, Aggregation aggregation)
			throws IOException {
		List<AggregateDatum> result = new ArrayList<>();
		try (ICsvListReader r = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			r.getHeader(true);
			List<String> row = null;
			while ( (row = r.read()) != null ) {
				if ( row.size() < 8 ) {
					continue;
				}
				final UUID streamId = UUID.fromString(row.get(0));
				final Instant ts = parseInstant(row.get(1));

				String[] iCol = parseArrayValue(row.get(2));
				String[] aCol = parseArrayValue(row.get(3));
				String[] sCol = parseArrayValue(row.get(4));
				String[] tCol = parseArrayValue(row.get(5));

				DatumProperties props = DatumProperties.propertiesOf(decimalArray(iCol),
						decimalArray(aCol), sCol, tCol);

				String[][] statsCol = parse2dArrayValue(row.get(6));
				String[][] readingStatsCol = parse2dArrayValue(row.get(7));

				DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(
						parase2dDecimalArray(statsCol), parase2dDecimalArray(readingStatsCol));

				AggregateDatumEntity d = new AggregateDatumEntity(streamId, ts, aggregation, props,
						stats);
				result.add(d);
			}
		}
		return result;
	}

	/**
	 * Get an iterator for Datum parsed from a classpath UTF-8 CSV file.
	 * 
	 * @param clazz
	 *        the class to load the UTF-8 encoded CSV resource from
	 * @param resource
	 *        the CSV resource to load; if not found an empty iterator will be
	 *        returned
	 * @param metaProvider
	 *        the metadata provider
	 * @param formatter
	 *        the formatter; if {@literal null} then a standard ISO 8601
	 *        timestamp will be assumed
	 * @return the iterator, never {@literal null}
	 * @since 2.0
	 * 
	 */
	public static CloseableIterator<Datum> datumResourceIterator(Class<?> clazz, String resource,
			ObjectDatumStreamMetadataProvider metaProvider, DateTimeFormatter formatter) {
		try {
			InputStream is = clazz.getResourceAsStream(resource);
			if ( is == null ) {
				return new CloseableIterator<Datum>() {

					@Override
					public void close() throws IOException {
						// nothing
					}

					@Override
					public Datum next() {
						throw new NoSuchElementException();
					}

					@Override
					public boolean hasNext() {
						return false;
					}
				};
			}
			return new DatumCsvIterator(
					new CsvListReader(new InputStreamReader(is, ByteUtils.UTF8),
							CsvPreference.STANDARD_PREFERENCE),
					metaProvider, formatter != null ? formatter : DateTimeFormatter.ISO_INSTANT);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a list of Datum parsed from a classpath CSV file.
	 * 
	 * @param clazz
	 *        the class to load the CSV resource from
	 * @param resource
	 *        the CSV resource to load
	 * @param metaProvider
	 *        the metadata provider
	 * @return the list
	 * @throws RuntimeException
	 *         if any parsing error occurs
	 * @since 2.0
	 */
	public static List<Datum> datumResourceList(Class<?> clazz, String resource,
			ObjectDatumStreamMetadataProvider metaProvider, DateTimeFormatter formatter) {
		List<Datum> result = new ArrayList<>();
		try (CloseableIterator<Datum> itr = datumResourceIterator(clazz, resource, metaProvider,
				formatter)) {
			while ( itr.hasNext() ) {
				result.add(itr.next());
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return result;
	}

}
