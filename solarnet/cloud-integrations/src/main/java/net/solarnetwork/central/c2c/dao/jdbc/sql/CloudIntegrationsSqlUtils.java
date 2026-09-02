/* ==================================================================
 * CloudIntegrationsSqlUtils.java - 24/07/2026 2:43:11 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import net.solarnetwork.central.common.dao.SourceCriteria;
import net.solarnetwork.central.datum.support.DatumUtils;

/**
 * SQL helper methods for cloud integrations.
 *
 * @author matt
 * @version 1.0
 */
public final class CloudIntegrationsSqlUtils {

	private CloudIntegrationsSqlUtils() {
		// not available
	}

	/**
	 * Generate a SQL {@code WITH} clause for a source IDs array JDBC parameter.
	 *
	 * @param filter
	 *        the source ID criteria
	 * @param buf
	 *        the output buffer to append the SQL into
	 * @return the number of JDBC parameters generated
	 * @see #joinCloudDatumStreamSourceIdsFilter(SourceCriteria, StringBuilder)
	 * @see #whereCloudDatumStreamHasSourceIds(SourceCriteria, StringBuilder)
	 */
	public static int withCloudDatumStreamSourceIdsFilter(SourceCriteria filter, StringBuilder buf) {
		if ( !filter.hasSourceCriteria() ) {
			return 0;
		}
		buf.append("""
				WITH sources AS (
					SELECT ?::text[] AS source_ids
				)
				""");
		return 1;
	}

	/**
	 * Generate a SQL {@code JOIN} clause with the {@code sources} table.
	 *
	 * @param filter
	 *        the source ID criteria
	 * @param buf
	 *        the output buffer to append the SQL into
	 * @see #withCloudDatumStreamSourceIdsFilter(SourceCriteria, StringBuilder)
	 * @see #whereCloudDatumStreamHasSourceIds(SourceCriteria, StringBuilder)
	 */
	public static void joinCloudDatumStreamSourceIdsFilter(SourceCriteria filter, StringBuilder buf) {
		if ( filter.hasSourceCriteria() ) {
			buf.append("""
					INNER JOIN sources ON TRUE
					""");
		}
	}

	/**
	 * Generate a SQL {@code WHERE} clause to match a set of source ID patterns
	 * against the source IDs in the cloud datum streams table.
	 *
	 * <p>
	 * The Cloud Datum Stream table prefix is assumed to be {@code cds}. A
	 * {@code sources.source_ids} column is presumed, as from
	 * {@link #withCloudDatumStreamSourceIdsFilter(SourceCriteria, StringBuilder)}.
	 * </p>
	 *
	 * <p>
	 * If any SQL is added to {@code buf} it will start with {@code \tAND }.
	 * </p>
	 *
	 * @param filter
	 *        the source ID criteria
	 * @param buf
	 *        the output buffer to append the SQL into
	 * @return the number of JDBC parameters generated
	 * @see #withCloudDatumStreamSourceIdsFilter(SourceCriteria, StringBuilder)
	 * @see #joinCloudDatumStreamSourceIdsFilter(SourceCriteria, StringBuilder)
	 */
	public static int whereCloudDatumStreamHasSourceIds(SourceCriteria filter, StringBuilder buf) {
		if ( !filter.hasSourceCriteria() ) {
			return 0;
		}
		boolean pattern = false;
		for ( String sourceId : filter.sourceIds() ) {
			if ( DatumUtils.WILDCARD_PATTERN_MATCHER.isPattern(sourceId) ) {
				pattern = true;
				break;
			}
		}
		buf.append("\tAND (\n");
		if ( pattern ) {
			// @formatter:off
			buf.append(
					"""
						cds.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
						OR (
							jsonb_typeof(cds.sprops->'sourceIdMap') = 'object'
							AND EXISTS (
								SELECT TRUE
								FROM jsonb_array_elements_text(jsonb_path_query_array(cds.sprops->'sourceIdMap', '$.*')) AS m_source_id
								WHERE m_source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
							)
						)
						OR 	(
							jsonb_typeof(cds.sprops->'virtualSourceIds') = 'array'
							AND jsonb_array_length(cds.sprops->'virtualSourceIds') > 0
							AND EXISTS (
								SELECT TRUE
								FROM jsonb_array_elements_text(cds.sprops->'virtualSourceIds') AS v_source_id
								WHERE v_source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
							)
						)
					""");
			// @formatter:on
		} else {
			// @formatter:off
			buf.append(
					"""
						cds.source_id = ANY(sources.source_ids)
						OR (
							jsonb_typeof(cds.sprops->'sourceIdMap') = 'object'
							AND EXISTS (
								SELECT TRUE
								FROM jsonb_array_elements_text(jsonb_path_query_array(cds.sprops->'sourceIdMap', '$.*')) AS m_source_id
								WHERE m_source_id = ANY(sources.source_ids)
							)
						)
						OR 	(
							jsonb_typeof(cds.sprops->'virtualSourceIds') = 'array'
							AND jsonb_array_length(cds.sprops->'virtualSourceIds') > 0
							AND EXISTS (
								SELECT TRUE
								FROM jsonb_array_elements_text(cds.sprops->'virtualSourceIds') AS v_source_id
								WHERE v_source_id = ANY(sources.source_ids)
							)
						)
					""");
			// @formatter:on
		}
		buf.append(")\n");
		return 0;
	}

}
