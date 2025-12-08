/* ==================================================================
 * SigenergyFields.java - 9/12/2025 11:09:36 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.sigen;

import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.core.io.Resource;
import org.supercsv.comment.CommentStartsWith;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.c2c.domain.CloudDataValue;

/**
 * Sigenergy fields database.
 *
 * @author matt
 * @version 1.0
 */
public class SigenergyFields {

	/** The field set name for the AIO group. */
	public static final String AIO_FIELD_SET_NAME = "aio";

	/** The field set name for the gateway group. */
	public static final String GATEWAY_FIELD_SET_NAME = "gateway";

	/** The field set name for the meter group. */
	public static final String METER_FIELD_SET_NAME = "meter";

	/** The field set name for the system summary group. */
	public static final String SYS_SUMMERY_FIELD_SET_NAME = "sys_summary";

	/** The field set name for the system energy flow group. */
	public static final String SYS_ENERGY_FLOW_FIELD_SET_NAME = "sys_energyflow";

	/**
	 * Field information.
	 */
	public static record FieldInfo(String fieldName, String key, String description, String unit,
			BigDecimal scaleFactor) {

		/**
		 * Get a cloud data value for this field.
		 *
		 * @param systemId
		 *        the system identifier
		 * @param deviceId
		 *        the device identifier
		 * @return the cloud data value, never {@code null}
		 */
		public CloudDataValue cloudDataValue(String systemId, String deviceId) {
			Map<String, Object> meta = new LinkedHashMap<>(3);
			if ( description != null ) {
				meta.put(CloudDataValue.DESCRIPTION_METADATA, description);
			}
			if ( unit != null ) {
				meta.put(CloudDataValue.UNIT_OF_MEASURE_METADATA, unit);
			}
			if ( scaleFactor != null ) {
				meta.put(CloudDataValue.UNIT_SCALE_FACTOR_METADATA, scaleFactor);
			}
			return dataValue(List.of(systemId, deviceId, key), fieldName, !meta.isEmpty() ? meta : null);
		}
	}

	private final Map<String, List<FieldInfo>> fieldSets;

	/**
	 * Constructor.
	 *
	 * @param resources
	 *        the resources to load; the keys represent set names; the values
	 *        must provide UTF-8 CSV field information
	 */
	public SigenergyFields(Map<String, Resource> resources) {
		super();
		final Map<String, List<FieldInfo>> fields = new LinkedHashMap<>(
				requireNonNullArgument(resources, "resources").size());
		for ( Entry<String, Resource> e : resources.entrySet() ) {
			fields.put(e.getKey(), loadFieldSet(e.getKey(), e.getValue()));
		}
		this.fieldSets = Collections.unmodifiableMap(fields);
	}

	private List<FieldInfo> loadFieldSet(final String setKey, final Resource value) {
		final List<FieldInfo> result = new ArrayList<>(8);
		final CsvPreference prefs = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
				.skipComments(new CommentStartsWith("#")).build();
		try (ICsvListReader in = new CsvListReader(
				new InputStreamReader(value.getInputStream(), StandardCharsets.UTF_8), prefs)) {

			@SuppressWarnings("unused")
			var unused = in.getHeader(true);

			List<String> row = null;
			while ( (row = in.read()) != null ) {
				if ( row.isEmpty() || row.size() < 2 ) {
					continue;
				}
				String fieldName = nonEmptyString(row.get(0));
				String key = nonEmptyString(row.get(1));
				if ( fieldName == null || key == null ) {
					continue;
				}
				BigDecimal scaleFactor = null;
				if ( row.size() > 4 ) {
					String sf = nonEmptyString(row.get(4));
					if ( sf != null ) {
						try {
							scaleFactor = new BigDecimal(sf);
						} catch ( NumberFormatException e ) {
							// ignore and continue
						}
					}
				}
				result.add(new FieldInfo(fieldName, key, row.get(2), row.get(3), scaleFactor));
			}
		} catch ( IOException e ) {
			throw new RuntimeException("Failed to load field set [%s] from %s".formatted(setKey, value),
					e);
		}
		return result;
	}

	/**
	 * Get the field sets.
	 *
	 * @return the field sets
	 */
	public Map<String, List<FieldInfo>> getFieldSets() {
		return fieldSets;
	}

	/**
	 * Get a list of cloud data values for a field set.
	 *
	 * @param setKey
	 *        the set key
	 * @param systemId
	 *        the system identifier
	 * @param deviceId
	 *        the device identifier
	 * @return the cloud data values, never {@code null}
	 */
	public List<CloudDataValue> cloudDataValues(String setKey, String systemId, String deviceId) {
		List<FieldInfo> fields = fieldSets.get(setKey);
		if ( fields == null ) {
			return List.of();
		}
		List<CloudDataValue> result = new ArrayList<>(fields.size());
		for ( FieldInfo field : fields ) {
			result.add(field.cloudDataValue(systemId, deviceId));
		}
		return result;
	}

	/**
	 * Get a merged list of cloud data values for multiple field sets.
	 *
	 * @param systemId
	 *        the system identifier
	 * @param deviceId
	 *        the device identifier
	 * @param setKeys
	 *        the set keys
	 * @return the cloud data values, never {@code null}
	 */
	public List<CloudDataValue> mergedCloudDataValues(String systemId, String deviceId,
			String... setKeys) {
		final Map<String, FieldInfo> merged = new LinkedHashMap<>(8);
		for ( String setKey : setKeys ) {
			List<FieldInfo> fields = fieldSets.get(setKey);
			if ( fields == null ) {
				continue;
			}
			for ( FieldInfo field : fields ) {
				merged.putIfAbsent(field.key, field);
			}
		}
		if ( merged.isEmpty() ) {
			return List.of();
		}
		List<CloudDataValue> result = new ArrayList<>(merged.size());
		for ( FieldInfo field : merged.values() ) {
			result.add(field.cloudDataValue(systemId, deviceId));
		}
		return result;
	}

}
