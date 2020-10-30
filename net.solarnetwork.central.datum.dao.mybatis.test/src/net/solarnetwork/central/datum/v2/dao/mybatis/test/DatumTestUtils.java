/* ==================================================================
 * DatumTestUtils.java - 30/10/2020 2:26:18 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Helper methods for datum tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class DatumTestUtils {

	private DatumTestUtils() {
		// don't construct me
	}

	/**
	 * Create a {@link NodeDatumStreamMetadata} out of a collection of
	 * {@link GeneralNodeDatum} instances.
	 * 
	 * @param datums
	 *        the datums
	 * @param nspk
	 *        the specific node+source to create the metadata for
	 * @return the metadata
	 */
	public static NodeDatumStreamMetadata createMetadata(Iterable<GeneralNodeDatum> datums,
			NodeSourcePK nspk) {
		Set<String> iNames = new LinkedHashSet<>(4);
		Set<String> aNames = new LinkedHashSet<>(4);
		Set<String> sNames = new LinkedHashSet<>(4);
		for ( GeneralNodeDatum d : datums ) {
			if ( d.getSamples() == null || !(d.getNodeId().equals(nspk.getNodeId())
					&& d.getSourceId().equals(nspk.getSourceId())) ) {
				continue;
			}
			GeneralDatumSamples s = d.getSamples();
			if ( s.getInstantaneous() != null ) {
				iNames.addAll(s.getInstantaneous().keySet());
			}
			if ( s.getAccumulating() != null ) {
				aNames.addAll(s.getAccumulating().keySet());
			}
			if ( s.getStatus() != null ) {
				sNames.addAll(s.getStatus().keySet());
			}
		}
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), nspk.getNodeId(), nspk.getSourceId(),
				iNames.isEmpty() ? null : iNames.toArray(new String[iNames.size()]),
				aNames.isEmpty() ? null : aNames.toArray(new String[aNames.size()]),
				sNames.isEmpty() ? null : sNames.toArray(new String[sNames.size()]));
	}

	/**
	 * Insert a set of datum into the {@literal da_datm} table.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param datums
	 *        the datum to insert
	 * @return the resulting stream metadata
	 */
	public static Map<NodeSourcePK, NodeDatumStreamMetadata> insertDatum(Logger log,
			JdbcTemplate jdbcTemplate, Iterable<GeneralNodeDatum> datums) {
		final Map<NodeSourcePK, NodeDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"insert into solardatm.da_datm (stream_id,ts,received,data_i,data_a,data_s,data_t) "
								+ "VALUES (?::uuid,?,?,?::numeric[],?::numeric[],?::text[],?::text[])")) {
					final Timestamp now = Timestamp.from(Instant.now());
					for ( GeneralNodeDatum d : datums ) {
						final GeneralDatumSamples s = d.getSamples();
						if ( s == null || s.isEmpty() ) {
							continue;
						}
						if ( log != null ) {
							log.debug("Inserting Datum {}", d);
						}

						NodeSourcePK nspk = new NodeSourcePK(d.getNodeId(), d.getSourceId());
						NodeDatumStreamMetadata meta = result.computeIfAbsent(nspk, k -> {
							return createMetadata(datums, k);
						});
						datumStmt.setString(1, meta.getStreamId().toString());
						datumStmt.setTimestamp(2,
								Timestamp.from(Instant.ofEpochMilli(d.getCreated().getMillis())));
						datumStmt.setTimestamp(3, now);

						String[] iNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
						if ( iNames == null || iNames.length < 1 ) {
							datumStmt.setNull(4, Types.OTHER);
						} else {
							BigDecimal[] numbers = new BigDecimal[iNames.length];
							for ( int i = 0; i < iNames.length; i++ ) {
								numbers[i] = s.getInstantaneousSampleBigDecimal(iNames[i]);
							}
							Array iArray = con.createArrayOf("NUMERIC", numbers);
							datumStmt.setArray(4, iArray);
						}

						String[] aNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
						if ( aNames == null || aNames.length < 1 ) {
							datumStmt.setNull(5, Types.OTHER);
						} else {
							BigDecimal[] numbers = new BigDecimal[aNames.length];
							for ( int i = 0; i < aNames.length; i++ ) {
								numbers[i] = s.getAccumulatingSampleBigDecimal(aNames[i]);
							}
							Array aArray = con.createArrayOf("NUMERIC", numbers);
							datumStmt.setArray(5, aArray);
						}

						String[] sNames = meta.propertyNamesForType(GeneralDatumSamplesType.Status);
						if ( sNames == null || sNames.length < 1 ) {
							datumStmt.setNull(6, Types.OTHER);
						} else {
							String[] strings = new String[sNames.length];
							for ( int i = 0; i < sNames.length; i++ ) {
								strings[i] = s.getStatusSampleString(sNames[i]);
							}
							Array aArray = con.createArrayOf("TEXT", strings);
							datumStmt.setArray(6, aArray);
						}

						Set<String> tags = s.getTags();
						if ( tags == null || tags.isEmpty() ) {
							datumStmt.setNull(7, Types.OTHER);
						} else {
							String[] strings = tags.toArray(new String[tags.size()]);
							Array aArray = con.createArrayOf("TEXT", strings);
							datumStmt.setArray(7, aArray);
						}

						datumStmt.execute();
					}
				}
				try (PreparedStatement metaStmt = con.prepareStatement(
						"insert into solardatm.da_datm_meta (stream_id,node_id,source_id,names_i,names_a,names_s) "
								+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])")) {
					for ( NodeDatumStreamMetadata meta : result.values() ) {
						if ( log != null ) {
							log.debug("Inserting NodeDatumStreamMetadata {}", meta);
						}
						metaStmt.setString(1, meta.getStreamId().toString());
						metaStmt.setObject(2, meta.getNodeId());
						metaStmt.setString(3, meta.getSourceId());

						String[] iNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
						if ( iNames == null || iNames.length < 1 ) {
							metaStmt.setNull(4, Types.OTHER);
						} else {
							Array iArray = con.createArrayOf("TEXT", iNames);
							metaStmt.setArray(4, iArray);
						}

						String[] aNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
						if ( aNames == null || aNames.length < 1 ) {
							metaStmt.setNull(5, Types.OTHER);
						} else {
							Array aArray = con.createArrayOf("TEXT", aNames);
							metaStmt.setArray(5, aArray);
						}

						String[] sNames = meta.propertyNamesForType(GeneralDatumSamplesType.Status);
						if ( sNames == null || sNames.length < 1 ) {
							metaStmt.setNull(6, Types.OTHER);
						} else {
							Array aArray = con.createArrayOf("TEXT", sNames);
							metaStmt.setArray(6, aArray);
						}

						metaStmt.execute();
					}
				}
				return null;
			}
		});
		return result;
	}

}
