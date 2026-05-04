/* ==================================================================
 * JdbcDatumBulkLoadingSupport.java - 3/05/2026 6:18:02 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.StreamRange;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Support for bulk loading datum.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumBulkLoadingSupport {

	/**
	 * The {@code storeDatumJdbcCall} property default value.
	 */
	public static final String DEFAULT_STORE_DATUM_JDBC_CALL = "{? = call solardatm.store_datum(?,?,?,?,?,?)}";

	/**
	 * The {@code storeDatumJdbcCall} property default value.
	 */
	public static final String DEFAULT_STORE_LOCATION_DATUM_JDBC_CALL = "{? = call solardatm.store_loc_datum(?,?,?,?,?,?)}";

	/**
	 * The {@code storeStreamDatumJdbcCall} property default value.
	 */
	public static final String DEFAULT_STORE_STREAM_DATUM_JDBC_CALL = "{call solardatm.store_stream_datum(?,?,?,?,?,?,?,?)}";

	/**
	 * The {@code markStaleDatumHoursJdbcCall} property default value.
	 */
	public static final String DEFAULT_MARK_STALE_DATUM_HOURS_JDBC_CALL = "{call solardatm.mark_stale_datm_hours(?,?,?)}";

	/**
	 * The {@code auditIncrementDatumCountJdbcCall} property default value.
	 */
	public static final String DEFAULT_AUDIT_INCREMENT_DATUM_COUNT_CALL = "{call solardatm.audit_increment_datum_count(?,?,?,?)}";

	private final InstantSource clock;
	private final boolean track;
	private String markStaleDatumHoursJdbcCall = DEFAULT_MARK_STALE_DATUM_HOURS_JDBC_CALL;
	private String auditIncrementDatumCountJdbcCall = DEFAULT_AUDIT_INCREMENT_DATUM_COUNT_CALL;
	private @Nullable DatumStreamMetadataDao datumStreamMetadataDao;

	private final Map<UUID, StreamBulkLoadStats> streamStats = new HashMap<>(32);

	// cache datum stream  metadata to speed up conversion to StreamDatum
	private final Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> streamMetaCache = new HashMap<>(
			32);

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@code null}
	 */
	public JdbcDatumBulkLoadingSupport(InstantSource clock) {
		this(clock, false);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param track
	 *        {@code true} to track changes at each store, rather than at end
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@code null}
	 */
	public JdbcDatumBulkLoadingSupport(InstantSource clock, boolean track) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.track = track;
	}

	/**
	 * Track statistics during bulk import of a datum stream.
	 */
	public static class StreamBulkLoadStats extends StreamRange {

		private ObjectDatumKind streamKind;
		private int dcount;
		private int pcount;

		/**
		 * Constructor.
		 *
		 * @param streamId
		 *        the stream ID
		 * @param streamKind
		 *        the stream kind
		 */
		public StreamBulkLoadStats(UUID streamId, ObjectDatumKind streamKind) {
			super(streamId);
			this.streamKind = requireNonNullArgument(streamKind, "streamKind");
		}

		/**
		 * Add statistics for a given datum.
		 *
		 * @param ts
		 *        the datum timestamp
		 * @param samples
		 *        the datum samples to calculate the datum/property counts from
		 */
		public void updateStatsForDatum(final Instant ts, DatumSamplesOperations samples) {
			if ( getStartDate() == null || ts.isBefore(getStartDate()) ) {
				setStartDate(ts);
			}
			if ( getEndDate() == null || ts.isAfter(getEndDate()) ) {
				setEndDate(ts);
			}

			dcount += 1;

			addPropCounts(samples.getSampleData(DatumSamplesType.Instantaneous));
			addPropCounts(samples.getSampleData(DatumSamplesType.Accumulating));
			addPropCounts(samples.getSampleData(DatumSamplesType.Status));
			Set<String> tags = samples.getTags();
			if ( tags != null ) {
				pcount += tags.size();
			}
		}

		private void addPropCounts(@Nullable Map<?, ?> m) {
			if ( m != null ) {
				pcount += m.size();
			}
		}

		/**
		 * Add statistics for a given datum.
		 *
		 * @param d
		 *        the datum
		 */
		public void updateStatsForDatum(final StreamDatum d) {
			final Instant ts = d.getTimestamp();

			if ( getStartDate() == null || ts.isBefore(getStartDate()) ) {
				setStartDate(ts);
			}
			if ( getEndDate() == null || ts.isAfter(getEndDate()) ) {
				setEndDate(ts);
			}

			dcount += 1;
			pcount += d.getProperties().getLength();
		}

	}

	/**
	 * Store a datum.
	 *
	 * @param datum
	 *        the datum to store
	 * @param stmt
	 *        the statement to use; this should be the same statement passed to
	 *        {@link #storeDatum(GeneralObjectDatum,CallableStatement, Timestamp)}
	 * @param receivedDefault
	 *        the received timestamp default value
	 * @return {@code true} if the datum was stored
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public boolean storeDatum(Datum datum, CallableStatement stmt, Timestamp receivedDefault)
			throws SQLException {
		final GeneralObjectDatum<?> d = DatumUtils.convertGeneralDatum(datum,
				receivedDefault.toInstant());
		if ( d == null ) {
			return false;
		}
		return storeDatum(d, stmt, receivedDefault);
	}

	/**
	 * Store a general datum.
	 *
	 * @param d
	 *        the datum to store
	 * @param stmt
	 *        the statement to use
	 * @param receivedDefault
	 *        the fallback received date to use, if the datum does not provide
	 *        one
	 * @return {@code true} if the datum was stored
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @throws IllegalStateException
	 *         if {@code d} is not a supported type
	 */
	public boolean storeDatum(GeneralObjectDatum<?> d, CallableStatement stmt, Timestamp receivedDefault)
			throws SQLException {
		stmt.setTimestamp(2, Timestamp.from(d.getCreated()));
		stmt.setLong(3, d.getObjectId());
		stmt.setString(4, d.getSourceId());

		final Timestamp received;
		final String sampleJson;
		if ( d instanceof GeneralNodeDatum nd ) {
			received = (nd.getPosted() != null ? Timestamp.from(nd.getPosted()) : receivedDefault);
			sampleJson = nd.getSampleJson();
		} else if ( d instanceof GeneralLocationDatum ld ) {
			received = (ld.getPosted() != null ? Timestamp.from(ld.getPosted()) : receivedDefault);
			sampleJson = ld.getSampleJson();
		} else {
			throw new IllegalStateException(
					"GeneralObjectDatum object [%s] is not a supported type".formatted(d.getClass()));
		}

		stmt.setTimestamp(5, received);
		stmt.setString(6, sampleJson);
		stmt.setBoolean(7, track);
		stmt.executeUpdate();

		final UUID streamId = CommonJdbcUtils.getUuid(stmt, 1);
		if ( streamId == null ) {
			return false;
		}

		if ( track ) {
			// not deferring tracking, so no need to maintain stats
			return true;
		}

		updateStatsForDatum(d, d.getSamples(), streamId);

		return true;
	}

	/**
	 * Store a stream datum.
	 *
	 * @param kind
	 *        the datum stream kind
	 * @param d
	 *        the datum to store
	 * @param stmt
	 *        the statement to use
	 * @param receivedDefault
	 *        the fallback received date to use, if the datum does not provide
	 *        one
	 * @return {@code true} if the datum was stored
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public boolean storeDatum(ObjectDatumKind kind, StreamDatum d, CallableStatement stmt,
			Timestamp receivedDefault) throws SQLException {
		stmt.setObject(1, d.streamId(), Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));

		final Timestamp received;
		if ( d instanceof DatumEntity de ) {
			received = (de.getReceived() != null ? Timestamp.from(de.getReceived()) : receivedDefault);
		} else {
			received = receivedDefault;
		}

		stmt.setTimestamp(3, received);

		final DatumProperties p = d.getProperties();

		// NOTE that prepareArrayParameter uses 0-based parameter offsets, unlike direct JDBC
		CommonSqlUtils.prepareArrayParameter(stmt.getConnection(), stmt, 3,
				p.getInstantaneousLength() > 0 ? p.getInstantaneous() : null, true);
		CommonSqlUtils.prepareArrayParameter(stmt.getConnection(), stmt, 4,
				p.getAccumulatingLength() > 0 ? p.getAccumulating() : null, true);
		CommonSqlUtils.prepareArrayParameter(stmt.getConnection(), stmt, 5,
				p.getStatusLength() > 0 ? p.getStatus() : null, true);
		CommonSqlUtils.prepareArrayParameter(stmt.getConnection(), stmt, 6,
				p.getTagsLength() > 0 ? p.getTags() : null, true);

		stmt.setBoolean(8, track);
		stmt.executeUpdate();

		if ( track ) {
			// not deferring tracking, so no need to maintain stats
			return true;
		}

		updateStatsForDatum(kind, d);

		return true;
	}

	/**
	 * Update the stream statistics for a persisted datum.
	 *
	 * @param datumId
	 *        the datum identity of the datum that was persisted
	 * @param samples
	 *        the datum samples that was persisted
	 * @param streamId
	 *        the stream ID
	 */
	public void updateStatsForDatum(DatumIdentity datumId, DatumSamplesOperations samples,
			UUID streamId) {
		// keep track of import min/max date ranges, so they can be updated later
		StreamBulkLoadStats stats = streamStats.compute(streamId,
				(k, v) -> (v != null ? v : new StreamBulkLoadStats(k, datumId.getKind())));
		stats.updateStatsForDatum(datumId.getTimestamp(), samples);
	}

	/**
	 * Update the stream statistics for a persisted stream datum.
	 *
	 * @param kind
	 *        the stream kind
	 * @param d
	 *        the datum that was persisted
	 */
	public void updateStatsForDatum(ObjectDatumKind kind, StreamDatum d) {
		// keep track of import min/max date ranges, so they can be updated later
		StreamBulkLoadStats stats = streamStats.compute(d.streamId(),
				(k, v) -> (v != null ? v : new StreamBulkLoadStats(k, kind)));
		stats.updateStatsForDatum(d);
	}

	/**
	 * Persist stream statistics.
	 *
	 * <p>
	 * This will call {@link #persistStreamStats(Connection, Collection)} will
	 * the values collected in {@link #getStreamStats()}, and then clear the
	 * map.
	 * </p>
	 *
	 * @param connection
	 *        a database connection
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public void persistStreamStats(Connection connection) throws SQLException {
		persistStreamStats(connection, streamStats.values());
		streamStats.clear();
	}

	/**
	 * Persist stream statistics.
	 *
	 * @param connection
	 *        a database connection
	 * @param stats
	 *        the statistics to persist
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public void persistStreamStats(Connection connection, Collection<StreamBulkLoadStats> stats)
			throws SQLException {
		if ( stats.isEmpty() ) {
			return;
		}
		try (CallableStatement auditStmt = connection.prepareCall(auditIncrementDatumCountJdbcCall);
				CallableStatement staleStmt = connection.prepareCall(markStaleDatumHoursJdbcCall)) {
			for ( StreamBulkLoadStats stat : stats ) {
				if ( stat.streamKind == ObjectDatumKind.Node ) {
					auditStmt.setObject(1, stat.getStreamId(), Types.OTHER);
					auditStmt.setTimestamp(2, Timestamp.from(clock.instant()));
					auditStmt.setInt(3, stat.dcount);
					auditStmt.setInt(4, stat.pcount);
					auditStmt.execute();
				}

				staleStmt.setObject(1, stat.getStreamId(), Types.OTHER);
				staleStmt.setTimestamp(2, Timestamp.from(stat.getStartDate()));
				staleStmt.setTimestamp(3, Timestamp.from(stat.getEndDate()));
				staleStmt.execute();
			}
		}
	}

	/**
	 * Try to convert a {@link Datum} into a {@link StreamDatum}.
	 *
	 * @param datumId
	 *        the datum identity
	 * @param datum
	 *        the datum to convert
	 * @return a {@link StreamDatum}, if possible to convert
	 */
	public @Nullable StreamDatum datumStreamDatum(DatumIdentity datumId, Datum datum) {
		final ObjectDatumStreamMetadata meta = datumStreamMetadata(datumId);
		if ( meta != null ) {
			try {
				var datumProps = DatumProperties.propertiesFrom(datum, meta);
				if ( datumProps != null ) {
					return new BasicStreamDatum(meta.getStreamId(), datumId.getTimestamp(), datumProps);
				}
			} catch ( IllegalArgumentException e ) {
				// incompatible properties for stream; fall back to generic datum
			}
		}
		return null;
	}

	/**
	 * Try to convert a {@link GeneralObjectDatum} into a {@link StreamDatum}.
	 *
	 * @param datum
	 *        the datum to convert
	 * @return a {@link StreamDatum}, if possible to convert
	 */
	public @Nullable StreamDatum datumStreamDatum(GeneralObjectDatum<?> datum) {
		final ObjectDatumStreamMetadata meta = datumStreamMetadata(datum);
		if ( meta != null ) {
			try {
				var d = new GeneralDatum(datum, datum.getSamples());
				var datumProps = DatumProperties.propertiesFrom(d, meta);
				if ( datumProps != null ) {
					return new BasicStreamDatum(meta.getStreamId(), datum.getTimestamp(), datumProps);
				}
			} catch ( IllegalArgumentException e ) {
				// incompatible properties for stream; fall back to generic datum
			}
		}
		return null;
	}

	private @Nullable ObjectDatumStreamMetadata datumStreamMetadata(DatumIdentity datumId) {
		final DatumStreamMetadataDao metaDao = getDatumStreamMetadataDao();
		if ( metaDao == null ) {
			return null;
		}
		final var metaId = new ObjectDatumStreamMetadataId(datumId.getKind(), datumId.getObjectId(),
				datumId.getSourceId());
		ObjectDatumStreamMetadata meta = streamMetaCache.get(metaId);
		if ( meta == null ) {
			var f = new BasicDatumCriteria();
			if ( datumId.getKind() == ObjectDatumKind.Location ) {
				f.setLocationId(datumId.getObjectId());
			} else {
				f.setNodeId(datumId.getObjectId());
			}
			f.setSourceId(datumId.getSourceId());
			for ( ObjectDatumStreamMetadata m : metaDao.findDatumStreamMetadata(f) ) {
				meta = m;
				streamMetaCache.put(metaId, meta);
				break;
			}
		}
		return meta;
	}

	/**
	 * Get the stream statistics.
	 *
	 * <p>
	 * This is a mapping of all stream statistics generated by calling
	 * {@code updateStatsForDatum(...)}.
	 * </p>
	 *
	 * @return the stream statistics
	 */
	public Map<UUID, StreamBulkLoadStats> getStreamStats() {
		return streamStats;
	}

	/**
	 * Get the "mark stale" JDBC call.
	 *
	 * @return the call; defaults to
	 *         {@link #DEFAULT_MARK_STALE_DATUM_HOURS_JDBC_CALL}
	 */
	public final String getMarkStaleDatumHoursJdbcCall() {
		return markStaleDatumHoursJdbcCall;
	}

	/**
	 * Set the "mark stale" JDBC call.
	 *
	 * @param markStaleDatumHoursJdbcCall
	 *        the call to set; if {@code null} then
	 *        {@link #DEFAULT_MARK_STALE_DATUM_HOURS_JDBC_CALL} will be used
	 */
	public final void setMarkStaleDatumHoursJdbcCall(String markStaleDatumHoursJdbcCall) {
		this.markStaleDatumHoursJdbcCall = (markStaleDatumHoursJdbcCall != null
				? markStaleDatumHoursJdbcCall
				: DEFAULT_MARK_STALE_DATUM_HOURS_JDBC_CALL);
	}

	/**
	 * Get the "update audit counts" JDBC call.
	 *
	 * @return the call; defaults to
	 *         {@link #DEFAULT_AUDIT_INCREMENT_DATUM_COUNT_CALL}
	 */
	public final String getAuditIncrementDatumCountJdbcCall() {
		return auditIncrementDatumCountJdbcCall;
	}

	/**
	 * Set the "update audit counts" JDBC call.
	 *
	 * @param auditIncrementDatumCountJdbcCall
	 *        the call to set; if {@code null} then
	 *        {@link #DEFAULT_AUDIT_INCREMENT_DATUM_COUNT_CALL} will be used
	 */
	public final void setAuditIncrementDatumCountJdbcCall(String auditIncrementDatumCountJdbcCall) {
		this.auditIncrementDatumCountJdbcCall = (auditIncrementDatumCountJdbcCall != null
				? auditIncrementDatumCountJdbcCall
				: DEFAULT_AUDIT_INCREMENT_DATUM_COUNT_CALL);
		;
	}

	/**
	 * Get the datum stream metadata DAO.
	 *
	 * @return the datum stream metadata DAO
	 */
	public @Nullable DatumStreamMetadataDao getDatumStreamMetadataDao() {
		return datumStreamMetadataDao;
	}

	/**
	 * Set the datum stream metadata DAO.
	 *
	 * @param datumStreamMetadataDao
	 *        the DAO to set
	 */
	public void setDatumStreamMetadataDao(@Nullable DatumStreamMetadataDao datumStreamMetadataDao) {
		this.datumStreamMetadataDao = datumStreamMetadataDao;
	}

}
