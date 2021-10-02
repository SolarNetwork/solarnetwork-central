/* ==================================================================
 * DatumCsvIterator.java - 8/11/2018 7:22:12 AM
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * Base class to support parsing CSV data into objects.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumCsvIterator implements Iterator<Datum>, Closeable {

	private final ICsvListReader reader;
	private final DateTimeFormatter dateFormatter;
	private final ObjectDatumStreamMetadataProvider metaProvider;
	private final Instant parseTime;

	private List<String> columnNames;
	private int dateColumn = -1;
	private int objectIdColumn = -1;
	private int sourceIdColumn = -1;
	private Map<String, Integer> columnMap;
	private Datum next;

	public DatumCsvIterator(ICsvListReader reader, ObjectDatumStreamMetadataProvider metaProvider)
			throws IOException {
		super();
		this.reader = reader;
		this.metaProvider = metaProvider;
		this.dateFormatter = DateTimeFormatter.ISO_INSTANT;
		this.parseTime = Instant.now();
	}

	@Override
	public void close() throws IOException {
		if ( reader != null ) {
			reader.close();
		}
	}

	private Datum getNext() {
		if ( next == null ) {
			try {
				// read in rows of data until we parse a non-null value
				List<String> row = null;
				do {
					row = reader.read();
					if ( row != null ) {
						if ( columnNames == null ) {
							setupColumns(row);
						} else {
							next = parseRow(row);
						}
					}
				} while ( next == null && row != null );
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		return next;
	}

	private void setupColumns(List<String> row) {
		this.columnNames = row;
		if ( row == null || row.isEmpty() ) {
			return;
		}
		Map<String, Integer> map = new LinkedHashMap<>(row.size());
		int i = -1;
		for ( String val : row ) {
			i++;
			map.put(val, i);
			if ( dateColumn < 0 && "created".equalsIgnoreCase(val) || "ts".equalsIgnoreCase(val)
					|| "date".equalsIgnoreCase(val) ) {
				dateColumn = i;
			} else if ( objectIdColumn < 0 && "nodeId".equalsIgnoreCase(val)
					|| "locationId".equalsIgnoreCase(val) ) {
				objectIdColumn = i;
			} else if ( sourceIdColumn < 0 && "sourceId".equalsIgnoreCase(val) ) {
				sourceIdColumn = i;
			}
		}
		if ( dateColumn < 0 ) {
			throw new IllegalArgumentException("No date column name found in header row.");
		}
		if ( objectIdColumn < 0 ) {
			throw new IllegalArgumentException("No object ID column name found in header row.");
		}
		if ( sourceIdColumn < 0 ) {
			throw new IllegalArgumentException("No source ID column name found in header row.");
		}
		this.columnMap = map;
	}

	@Override
	public boolean hasNext() {
		return (getNext() != null);
	}

	@Override
	public Datum next() {
		Datum result = getNext();
		if ( result != null ) {
			next = null;
		}
		return result;
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
	protected Datum parseRow(List<String> row) throws IOException {
		Instant ts = dateFormatter.parse(row.get(dateColumn), Instant::from);
		Long objectId = Long.valueOf(row.get(objectIdColumn));
		String sourceId = row.get(sourceIdColumn);
		ObjectDatumStreamMetadata meta = metaProvider.metadataForObjectSource(objectId, sourceId);
		if ( meta == null ) {
			// no meta available; ignore
			return null;
		}
		DatumProperties props = new DatumProperties();
		props.setInstantaneous(
				parseDecimalColummns(row, meta.propertyNamesForType(DatumSamplesType.Instantaneous)));
		props.setAccumulating(
				parseDecimalColummns(row, meta.propertyNamesForType(DatumSamplesType.Accumulating)));
		props.setStatus(parseStringColumns(row, meta.propertyNamesForType(DatumSamplesType.Status)));
		return new DatumEntity(meta.getStreamId(), ts, parseTime, props);
	}

	private String[] parseStringColumns(List<String> row, String[] propertyNames) {
		if ( propertyNames == null || propertyNames.length < 1 ) {
			return null;
		}
		boolean empty = true;
		String[] result = new String[propertyNames.length];
		for ( int i = 0, rowLen = row.size(); i < propertyNames.length; i++ ) {
			Integer idx = columnMap.get(propertyNames[i]);
			if ( idx != null && idx.intValue() < rowLen ) {
				String v = row.get(idx.intValue());
				if ( v != null ) {
					result[i] = v;
					empty = false;
				}
			}
		}
		return (empty ? null : result);
	}

	private BigDecimal[] parseDecimalColummns(List<String> row, String[] propertyNames) {
		if ( propertyNames == null || propertyNames.length < 1 ) {
			return null;
		}
		boolean empty = true;
		BigDecimal[] result = new BigDecimal[propertyNames.length];
		for ( int i = 0, rowLen = row.size(); i < propertyNames.length; i++ ) {
			Integer idx = columnMap.get(propertyNames[i]);
			if ( idx != null && idx.intValue() < rowLen ) {
				String v = row.get(idx.intValue());
				if ( v != null ) {
					result[i] = new BigDecimal(v);
					empty = false;
				}
			}
		}
		return (empty ? null : result);
	}

}
