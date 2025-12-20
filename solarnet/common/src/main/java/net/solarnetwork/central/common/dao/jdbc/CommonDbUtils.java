/* ==================================================================
 * CommonDbUtils.java - 16/12/2025 6:06:15 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.slf4j.Logger;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Utilities for working with common structures at the database level.
 *
 * <p>
 * These utilities are primarily designed to support unit testing.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonDbUtils {

	/**
	 * Get a SQL INSERT statement for the {@code solardatm.da_*_meta} table.
	 *
	 * <p>
	 * The order of columns is:
	 * </p>
	 *
	 * <ol>
	 * <li>stream_id (string as UUID)</li>
	 * <li>object_id (bigint)</li>
	 * <li>source_id (text)</li>
	 * <li>names_i (text[])</li>
	 * <li>names_a (text[])</li>
	 * <li>names_s (text[])</li>
	 * <li>jdata (string as JSONB)</li>
	 * </ol>
	 *
	 * @param kind
	 *        the meta kind
	 * @return the SQL
	 * @since 2.6
	 */
	public static String insertDatumMetaSql(ObjectDatumKind kind) {
		return """
				INSERT INTO solardatm.%s (stream_id,%s,source_id,names_i,names_a,names_s,jdata)
				VALUES (?::uuid,?,?,?::text[],?::text[],?::text[],?::jsonb)
				""".formatted(switch (kind) {
			case Location -> "da_loc_datm_meta";
			default -> "da_datm_meta";
		}, switch (kind) {
			case Location -> "loc_id";
			default -> "node_id";
		});
	}

	/**
	 * Insert node or location datum metadata.
	 *
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param metas
	 *        the metadata to insert
	 */
	public static void insertObjectDatumStreamMetadata(Logger log, JdbcOperations jdbcTemplate,
			Iterable<? extends ObjectDatumStreamMetadata> metas) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			insertObjectDatumStreamMetadata(log, con, metas);
			return null;
		});
	}

	/**
	 * Insert datum or location datum stream metadata.
	 *
	 * @param log
	 *        an optional logger
	 * @param con
	 *        the JDBC connection to use
	 * @param metas
	 *        the metadata to insert, can be either ndoe or location
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static void insertObjectDatumStreamMetadata(Logger log, Connection con,
			Iterable<? extends ObjectDatumStreamMetadata> metas) throws SQLException {
		try (PreparedStatement nodeMetaStmt = con
				.prepareStatement(insertDatumMetaSql(ObjectDatumKind.Node));
				PreparedStatement locMetaStmt = con
						.prepareStatement(insertDatumMetaSql(ObjectDatumKind.Location))) {
			for ( ObjectDatumStreamMetadata meta : metas ) {
				if ( log != null ) {
					log.debug("Inserting ObjectDatumStreamMetadata {}", meta);
				}
				@SuppressWarnings("resource")
				PreparedStatement metaStmt = (meta.getKind() == ObjectDatumKind.Location ? locMetaStmt
						: nodeMetaStmt);
				metaStmt.setString(1, meta.getStreamId().toString());
				metaStmt.setObject(2, meta.getObjectId());
				metaStmt.setString(3, meta.getSourceId());

				String[] iNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
				if ( iNames == null || iNames.length < 1 ) {
					metaStmt.setNull(4, Types.OTHER);
				} else {
					Array iArray = con.createArrayOf("TEXT", iNames);
					metaStmt.setArray(4, iArray);
				}

				String[] aNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
				if ( aNames == null || aNames.length < 1 ) {
					metaStmt.setNull(5, Types.OTHER);
				} else {
					Array aArray = con.createArrayOf("TEXT", aNames);
					metaStmt.setArray(5, aArray);
				}

				String[] sNames = meta.propertyNamesForType(DatumSamplesType.Status);
				if ( sNames == null || sNames.length < 1 ) {
					metaStmt.setNull(6, Types.OTHER);
				} else {
					Array aArray = con.createArrayOf("TEXT", sNames);
					metaStmt.setArray(6, aArray);
				}

				String json = meta.getMetaJson();
				metaStmt.setString(7, json);

				metaStmt.execute();
			}
		}
	}
}
