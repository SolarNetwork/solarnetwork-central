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

import java.io.IOException;
import java.io.Reader;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Utilities for Datum CSV processing.
 * 
 * @author matt
 * @version 1.0
 * @since 2.9
 */
public final class DatumCsvUtils {

	private DatumCsvUtils() {
		// don't construct me
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
	public static List<ObjectDatumStreamMetadata> parseCsvMetadata(Reader in, ObjectDatumKind kind,
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

	@SuppressWarnings("unchecked")
	private static <T> T parseArrayValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		if ( value.startsWith("{{") ) {
			return (T) parse2dArrayValue(value);
		}
		if ( value.startsWith("{") ) {
			value = value.substring(1);
		}
		if ( value.endsWith("}") ) {
			value = value.substring(0, value.length() - 1);
		}
		return (T) org.springframework.util.StringUtils.commaDelimitedListToStringArray(value);
	}

	private static String[][] parse2dArrayValue(String value) {
		// TODO Auto-generated method stub
		return null;
	}

}
