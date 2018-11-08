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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.supercsv.io.ICsvListReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Base class to support parsing CSV data into objects.
 * 
 * @author matt
 * @version 1.0
 * @param <E>
 *        the element type
 * @param <T>
 *        the properties type
 */
public abstract class BaseCsvIterator<E, T extends CsvDatumImportInputProperties>
		implements Iterator<E> {

	/** A type reference for a List with string keys. */
	public static final TypeReference<ArrayList<String>> STRING_LIST_TYPE = new StringListTypeReference();

	private static final class StringListTypeReference extends TypeReference<ArrayList<String>> {

		public StringListTypeReference() {
			super();
		}

	}

	private final ICsvListReader reader;
	protected final T props;
	protected final DateTimeFormatter dateFormatter;
	protected final ObjectMapper objectMapper;

	private E next;

	public BaseCsvIterator(ICsvListReader reader, T props) throws IOException {
		super();
		this.reader = reader;
		this.props = props;

		// skip past any header rows
		if ( props != null && props.getHeaderRowCount() != null ) {
			for ( int i = props.getHeaderRowCount(); i > 0; i-- ) {
				reader.read();
			}
		}
		DateTimeFormatter formatter = null;
		if ( props.getDateFormat() != null ) {
			try {
				formatter = DateTimeFormat.forPattern(props.getDateFormat());
				if ( props.getTimeZoneId() != null ) {
					formatter = formatter.withZone(DateTimeZone.forID(props.getTimeZoneId()));
				}
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( formatter == null ) {
			if ( props.getTimeZoneId() != null ) {
				formatter = ISODateTimeFormat.dateTimeParser();
			} else {
				formatter = ISODateTimeFormat.localDateOptionalTimeParser()
						.withZone(DateTimeZone.forID(props.getTimeZoneId()));
			}
		}
		this.dateFormatter = formatter;

		this.objectMapper = JsonUtils.newObjectMapper();
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
	protected abstract E parseRow(List<String> row) throws IOException;

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
	protected String getColumnValue(List<String> row, Integer col) {
		if ( row == null || col == null || col.intValue() > row.size() ) {
			return null;
		}
		return row.get(col - 1);
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
	protected String getColumnsValue(List<String> row, List<Integer> cols, String delimiter) {
		int numCols = cols.size();
		if ( row == null || cols == null || numCols < 1 ) {
			return null;
		}

		if ( numCols == 1 ) {
			return getColumnValue(row, cols.get(0));
		}

		StringBuilder buf = new StringBuilder();
		for ( Integer col : cols ) {
			String v = getColumnValue(row, col);
			if ( v == null ) {
				continue;
			}
			if ( buf.length() > 0 ) {
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
	protected Map<String, Object> parseMap(List<String> row, Integer col) {
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
	protected Map<String, Number> parseNumberMap(List<String> row, Integer col) {
		String v = getColumnValue(row, col);
		Map<String, Number> result = null;
		if ( v != null ) {
			Map<String, Object> m = JsonUtils.getStringMap(v);
			if ( m != null ) {
				result = m.entrySet().stream().filter(e -> e.getValue() instanceof Number)
						.collect(toMap(e -> e.getKey(), e -> (Number) e.getValue()));
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
	protected List<String> parseList(List<String> row, Integer col) {
		String v = getColumnValue(row, col);
		List<String> result = null;
		if ( v != null ) {
			try {
				result = objectMapper.readValue(v, STRING_LIST_TYPE);
			} catch ( IOException e ) {
				throw new DatumImportValidationException("Unable to parse JSON array from column " + col,
						e, reader.getLineNumber(), reader.getUntokenizedRow());
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
	protected Set<String> parseSet(List<String> row, Integer col) {
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
	protected GeneralNodeDatum parseDatum(List<String> row) {
		Long nodeId;
		try {
			nodeId = Long.valueOf(getColumnValue(row, props.getNodeIdColumn()));
		} catch ( NumberFormatException e ) {
			throw new DatumImportValidationException(
					"Error parsing node ID from column " + props.getNodeIdColumn() + ".", e,
					reader.getLineNumber(), reader.getUntokenizedRow());
		}

		String sourceId = getColumnValue(row, props.getSourceIdColumn());
		if ( sourceId == null ) {
			throw new DatumImportValidationException(
					"Unable to parse source ID from column " + props.getSourceIdColumn(),
					new NullPointerException(), reader.getLineNumber(), reader.getUntokenizedRow());
		}

		DateTime date;
		try {
			date = dateFormatter.parseDateTime(getColumnsValue(row, props.getDateColumns(), " "));
		} catch ( IllegalArgumentException e ) {
			throw new DatumImportValidationException("Error parsing date from columns "
					+ StringUtils.commaDelimitedStringFromCollection(props.getDateColumns()) + ".", e,
					reader.getLineNumber(), reader.getUntokenizedRow());
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(nodeId);
		d.setCreated(date);
		d.setSourceId(sourceId);
		return d;
	}

	private E getNext() {
		if ( next == null ) {
			try {
				// read in rows of data until we parse a non-null value
				List<String> row = null;
				do {
					row = reader.read();
					if ( row != null ) {
						next = parseRow(row);
					}
				} while ( next == null && row != null );
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
