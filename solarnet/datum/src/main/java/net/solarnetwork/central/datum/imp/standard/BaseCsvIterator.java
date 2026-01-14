/* ==================================================================
 * BaseCsvIterator.java - 8/11/2018 7:22:12 AM
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

import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.io.Closeable;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class to support parsing CSV data into objects.
 *
 * @param <E>
 *        the element type
 * @param <T>
 *        the properties type
 * @author matt
 * @version 3.0
 */
public abstract class BaseCsvIterator<E, T extends CsvDatumImportInputProperties>
		implements Iterator<E>, Closeable {

	/** A type reference for a List with string keys. */
	public static final TypeReference<ArrayList<String>> STRING_LIST_TYPE = new StringListTypeReference();

	private static final class StringListTypeReference extends TypeReference<ArrayList<String>> {

		private StringListTypeReference() {
			super();
		}

	}

	private static final DateTimeFormatter ISO_LOCAL_DATE_OPTIONAL_TIME;

	static {
		// @formatter:off
		ISO_LOCAL_DATE_OPTIONAL_TIME = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append(DateTimeFormatter.ISO_LOCAL_DATE)
				.optionalStart()
				.appendLiteral('T')
				.append(DateTimeFormatter.ISO_LOCAL_TIME)
				.toFormatter()
				.withChronology(IsoChronology.INSTANCE);
		// @formatter:on
	}

	private final CsvReader<CsvRecord> reader;
	private final Iterator<CsvRecord> delegate;
	protected final List<String> headerRow;
	protected final T props;
	protected final DateTimeFormatter dateFormatter;
	protected final ObjectMapper objectMapper;

	private E next;

	private static List<String> parseHeaderRows(Iterator<CsvRecord> delegate,
			CsvDatumImportInputProperties props) throws IOException {
		List<String> result = null;
		if ( props != null && props.getHeaderRowCount() != null ) {
			for ( int i = props.getHeaderRowCount(); i > 0 && delegate.hasNext(); i-- ) {
				CsvRecord row = delegate.next();
				if ( result == null ) {
					result = row.getFields();
				}
			}
		}
		return result;
	}

	public BaseCsvIterator(CsvReader<CsvRecord> reader, T props) throws IOException {
		super();
		this.reader = reader;
		this.delegate = reader.iterator();
		this.props = props;
		this.headerRow = parseHeaderRows(delegate, props);

		DateTimeFormatter formatter = null;
		if ( props.getDateFormat() != null ) {
			try {
				// @formatter:off
				formatter = new DateTimeFormatterBuilder().appendPattern(props.getDateFormat())
						.parseDefaulting(ChronoField.ERA, ChronoField.ERA.range().getMaximum())
						.toFormatter()
						.withResolverStyle(ResolverStyle.STRICT)
						.withChronology(IsoChronology.INSTANCE);
				// @formatter:on
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		if ( formatter == null ) {
			formatter = ISO_LOCAL_DATE_OPTIONAL_TIME;
		}
		if ( props.getTimeZoneId() != null && !props.getTimeZoneId().isEmpty() ) {
			formatter = formatter.withZone(ZoneId.of(props.getTimeZoneId()));
		}
		this.dateFormatter = formatter;

		this.objectMapper = JsonUtils.JSON_OBJECT_MAPPER;
	}

	/**
	 * Parse a single row of CSV data.
	 *
	 * @param row
	 *        the row data
	 * @return the parsed object, or {@literal null} to skip row and continue
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected abstract E parseRow(CsvRecord row) throws IOException;

	/**
	 * Get a column value.
	 *
	 * @param row
	 *        the row of column values
	 * @param col
	 *        the 1-based column number to get
	 * @return the column value, or {@literal null} if the column isn't
	 *         available
	 */
	protected String getColumnValue(CsvRecord row, Integer col) {
		if ( row == null || col == null || col > row.getFieldCount() || col < 1 ) {
			return null;
		}
		return row.getField(col - 1);
	}

	/**
	 * Get a combined column value.
	 *
	 * @param row
	 *        the row of column values
	 * @param cols
	 *        the 1-based column numbers to get
	 * @param delimiter
	 *        a delimiter to join the columns with
	 * @return the columns combined value, or {@literal null} if the columns
	 *         aren't available
	 */
	protected String getColumnsValue(CsvRecord row, List<Integer> cols, String delimiter) {
		int numCols = cols.size();
		if ( row == null || cols == null || numCols < 1 ) {
			return null;
		}

		if ( numCols == 1 ) {
			return getColumnValue(row, cols.getFirst());
		}

		StringBuilder buf = new StringBuilder();
		for ( Integer col : cols ) {
			String v = getColumnValue(row, col);
			if ( v == null ) {
				continue;
			}
			if ( !buf.isEmpty() ) {
				buf.append(delimiter);
			}
			buf.append(v);
		}

		return buf.toString();
	}

	/**
	 * Parse a JSON object string into a map arbitrary values.
	 *
	 * @param row
	 *        the data row
	 * @param col
	 *        the column to parse as a JSON map
	 * @return the map, or {@literal null} if the column is not available or the
	 *         resulting map would be empty
	 */
	protected Map<String, Object> parseMap(CsvRecord row, Integer col) {
		String v = getColumnValue(row, col);
		Map<String, Object> result = null;
		if ( v != null ) {
			result = JsonUtils.getStringMap(v);
			if ( result.isEmpty() ) {
				result = null;
			}
		}
		return result;
	}

	/**
	 * Parse a JSON object string into a map of number values.
	 *
	 * @param row
	 *        the data row
	 * @param col
	 *        the column to parse as a JSON number map
	 * @return the map, or {@literal null} if the column is not available or the
	 *         resulting map would be empty
	 */
	protected Map<String, Number> parseNumberMap(CsvRecord row, Integer col) {
		String v = getColumnValue(row, col);
		Map<String, Number> result = null;
		if ( v != null ) {
			Map<String, Object> m = JsonUtils.getStringMap(v);
			if ( m != null ) {
				result = m.entrySet().stream().filter(e -> e.getValue() instanceof Number)
						.collect(toMap(Map.Entry::getKey, e -> (Number) e.getValue()));
				if ( result.isEmpty() ) {
					result = null;
				}
			}
		}
		return result;
	}

	/**
	 * Parse a JSON array into a list of string values.
	 *
	 * @param row
	 *        the data row
	 * @param col
	 *        the column to parse as a JSON array
	 * @return the list, or {@literal null} if the column is not available or
	 *         the resulting list would be empty
	 */
	protected List<String> parseList(CsvRecord row, Integer col) {
		String v = getColumnValue(row, col);
		List<String> result = null;
		if ( v != null ) {
			try {
				result = objectMapper.readValue(v, STRING_LIST_TYPE);
			} catch ( JacksonException e ) {
				throw new DatumImportValidationException("Unable to parse JSON array from column " + col,
						e, row.getStartingLineNumber(),
						commaDelimitedStringFromCollection(row.getFields()));
			}
			if ( result.isEmpty() ) {
				result = null;
			}
		}
		return result;
	}

	/**
	 * Parse a JSON array into a set of string values.
	 *
	 * @param row
	 *        the data row
	 * @param col
	 *        the column to parse as a JSON array
	 * @return the list, or {@literal null} if the column is not available or
	 *         the resulting list would be empty
	 */
	protected Set<String> parseSet(CsvRecord row, Integer col) {
		List<String> l = parseList(row, col);
		Set<String> result = null;
		if ( l != null && !l.isEmpty() ) {
			result = new LinkedHashSet<>(l);
			if ( result.isEmpty() ) {
				result = null;
			}
		}
		return result;
	}

	/**
	 * Parse the basic properties of a datum from a row of data.
	 *
	 * <p>
	 * The following properties are parsed and set on the returned datum, using
	 * the columns configured on the provided
	 * {@link CsvDatumImportInputProperties} object:
	 * </p>
	 *
	 * <ul>
	 * <li>node ID</li>
	 * <li>source ID</li>
	 * <li>date</li>
	 * </ul>
	 *
	 * @param row
	 *        the row of data
	 * @return the datum
	 */
	protected GeneralNodeDatum parseDatum(CsvRecord row) {
		Long nodeId;
		try {
			nodeId = Long.valueOf(getColumnValue(row, props.nodeIdColumn()));
		} catch ( NumberFormatException e ) {
			throw new DatumImportValidationException(
					"Error parsing node ID from column " + props.getNodeIdColumn() + ".", e,
					row.getStartingLineNumber(), commaDelimitedStringFromCollection(row.getFields()));
		}

		String sourceId = getColumnValue(row, props.sourceIdColumn());
		if ( sourceId == null ) {
			throw new DatumImportValidationException(
					"Unable to parse source ID from column " + props.getSourceIdColumn(),
					new NullPointerException(), row.getStartingLineNumber(),
					commaDelimitedStringFromCollection(row.getFields()));
		}

		Instant date;
		try {
			if ( dateFormatter.getZone() != null ) {
				// guard against invalid DST date inputs here, to avoid ambiguity associated with
				// importing strictly invalid timestamps in a DST gap
				LocalDateTime dt = dateFormatter.parse(getColumnsValue(row, props.getDateColumns(), " "),
						LocalDateTime::from);
				ZoneId zone = dateFormatter.getZone();
				if ( !zone.getRules().isFixedOffset() ) {
					List<ZoneOffset> offsets = zone.getRules().getValidOffsets(dt);
					if ( offsets.isEmpty() ) {
						List<Integer> dateCols = props.getDateColumns();
						StringBuilder buf = new StringBuilder(
								"Date that cannot exist because of a daylight saving time transition in zone ");
						buf.append(zone.getId());
						buf.append(" on column");
						if ( dateCols.size() > 1 ) {
							buf.append("s");
						}
						buf.append(" ");
						buf.append(StringUtils.commaDelimitedStringFromCollection(dateCols));
						buf.append(".");
						throw new DatumImportValidationException(buf.toString(), null,
								row.getStartingLineNumber(),
								commaDelimitedStringFromCollection(row.getFields()));
					}
				}
				date = dt.atZone(zone).toInstant();
			} else {
				// assume date format includes zone
				ZonedDateTime dt = dateFormatter.parse(getColumnsValue(row, props.getDateColumns(), " "),
						ZonedDateTime::from);
				date = dt.toInstant();
			}
		} catch ( DateTimeParseException e ) {
			List<Integer> dateCols = props.getDateColumns();
			StringBuilder buf = new StringBuilder("Error parsing date from column");
			if ( dateCols.size() > 1 ) {
				buf.append("s");
			}
			buf.append(" ");
			buf.append(StringUtils.commaDelimitedStringFromCollection(dateCols));
			buf.append(".");
			throw new DatumImportValidationException(buf.toString(), e, row.getStartingLineNumber(),
					commaDelimitedStringFromCollection(row.getFields()));
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(nodeId);
		d.setCreated(date);
		d.setSourceId(sourceId);
		return d;
	}

	@Override
	public void close() throws IOException {
		if ( reader != null ) {
			reader.close();
		}
	}

	private E getNext() {
		if ( next == null && delegate.hasNext() ) {
			try {
				// read in rows of data until we parse a non-null value
				CsvRecord row;
				do {
					row = delegate.next();
					if ( row != null ) {
						next = parseRow(row);
					}
				} while ( next == null && delegate.hasNext() );
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		return next;
	}

	@Override
	public boolean hasNext() {
		return (getNext() != null);
	}

	@Override
	public E next() {
		E result = getNext();
		if ( result != null ) {
			next = null;
		}
		return result;
	}
}
