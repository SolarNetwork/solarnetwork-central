/* ==================================================================
 * DatumExpressionRoot.java - 12/11/2024 5:26:33 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import static java.util.Collections.emptyList;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Extension of {@link DatumSamplesExpressionRoot} that adds support for
 * {@link DatumMetadataOperations}.
 *
 * @author matt
 * @version 1.1
 */
public class DatumExpressionRoot extends DatumSamplesExpressionRoot {

	// a general metadata object, for example user metadata
	private final DatumMetadataOperations metadata;

	// a function to lookup metadata based on an object ID
	private final Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider;

	// a function to parse a metadata tariff schedule associated with an object ID
	private final BiFunction<DatumMetadataOperations, ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleProvider;

	private final DatumStreamsAccessor datumStreamsAccessor;

	/**
	 * Constructor.
	 *
	 * @param datum
	 *        the datum currently being populated
	 * @param sample
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @param metadata
	 *        the metadata
	 * @param datumStreamsAccessor
	 *        the datum streams accessor
	 * @param metadataProvider
	 *        function that resolves metadata based on an ID; the
	 *        {@code sourceId} component may be {@code null} to represent node
	 *        or location wide metadata
	 * @param tariffScheduleProvider
	 *        function that resolves a {@link TariffSchedule} from metadata
	 *        located at a path specified by
	 *        {@link ObjectDatumStreamMetadataId#getSourceId()}
	 */
	public DatumExpressionRoot(Datum datum, DatumSamplesOperations sample, Map<String, ?> parameters,
			DatumMetadataOperations metadata, DatumStreamsAccessor datumStreamsAccessor,
			Function<ObjectDatumStreamMetadataId, DatumMetadataOperations> metadataProvider,
			BiFunction<DatumMetadataOperations, ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleProvider) {
		super(datum, sample, parameters);
		this.metadata = metadata;
		this.datumStreamsAccessor = datumStreamsAccessor;
		this.metadataProvider = metadataProvider;
		this.tariffScheduleProvider = tariffScheduleProvider;
	}

	/**
	 * Create a copy with a given datum value.
	 *
	 * <p>
	 * The samples and parameters values will be set to {@literal null}.
	 * </p>
	 *
	 * @param datum
	 *        the datum
	 * @return a new instance using {@code datum}
	 */
	public DatumExpressionRoot copyWith(Datum datum) {
		return copyWith(datum, null, null);
	}

	/**
	 * Create a copy with a given datum, samples, and parameters values.
	 *
	 * @param datum
	 *        the datum
	 * @param samples
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @return the new instance using {@code datum}, {@code samples}, and
	 *         {@code parameters}
	 */
	public DatumExpressionRoot copyWith(Datum datum, DatumSamplesOperations samples,
			Map<String, ?> parameters) {
		return new DatumExpressionRoot(datum, samples, parameters, metadata, datumStreamsAccessor,
				metadataProvider, tariffScheduleProvider);
	}

	/**
	 * Get the general metadata.
	 *
	 * @return the general metadata, or {@code null} if none available
	 */
	public DatumMetadataOperations metadata() {
		return metadata;
	}

	/**
	 * Extract a value from the general metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object metadata(String path) {
		DatumMetadataOperations metadata = metadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Get node metadata associated with the configured {@code Datum}.
	 *
	 * @return the metadata for {@link #getDatum()}'s object ID, or {@code null}
	 *         if none available
	 */
	public DatumMetadataOperations nodeMetadata() {
		final Datum d = getDatum();
		return (d != null && d.getKind() == ObjectDatumKind.Node && metadataProvider != null
				? metadataProvider.apply(
						new ObjectDatumStreamMetadataId(d.getKind(), d.getObjectId(), null))
				: null);
	}

	/**
	 * Extract a value from the general metadata.
	 *
	 * @param path
	 *        the metadata path to extract
	 * @return the extracted metadata value, or {@code null} if none available
	 */
	public Object nodeMetadata(String path) {
		DatumMetadataOperations metadata = nodeMetadata();
		return (metadata != null ? metadata.metadataAtPath(path) : null);
	}

	/**
	 * Resolve a tariff schedule from node metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @return the schedule, or {@code node} if none available
	 */
	public TariffSchedule nodeTariffSchedule(String path) {
		final Datum d = getDatum();
		final DatumMetadataOperations meta = nodeMetadata();
		return (d != null && meta != null && tariffScheduleProvider != null
				? tariffScheduleProvider.apply(meta,
						new ObjectDatumStreamMetadataId(d.getKind(), d.getObjectId(), path))
				: null);
	}

	/**
	 * Resolve the first available tariff schedule rate for "now" from node
	 * metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @return the first available rate for the current time, or {@code null} if
	 *         not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path) {
		return resolveNodeTariffScheduleRate(path, LocalDateTime.now(), null);
	}

	/**
	 * Resolve the first available tariff schedule rate from node metadata at a
	 * given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @return the first available rate, or {@code null} if not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path, LocalDateTime date) {
		return resolveNodeTariffScheduleRate(path, date, null);
	}

	/**
	 * Resolve a tariff schedule rate from node metadata at a given path.
	 *
	 * @param path
	 *        the node metadata path to resolve the schedule at; the schedule
	 *        can be a CSV string or list of string arrays
	 * @param date
	 *        the date to evaluate the schedule at
	 * @param rateName
	 *        the name of the rate to return, or {@code null} to return the
	 *        first available rate
	 * @return the rate, or {@code null} if not available
	 */
	public BigDecimal resolveNodeTariffScheduleRate(String path, LocalDateTime date, String rateName) {
		BigDecimal result = null;
		TariffSchedule schedule = nodeTariffSchedule(path);
		if ( schedule != null ) {
			Tariff t = schedule.resolveTariff(date, null);
			if ( t != null ) {
				Map<String, Tariff.Rate> rates = t.getRates();
				if ( !rates.isEmpty() ) {
					Tariff.Rate r = (rateName != null ? rates.get(rateName)
							: rates.values().iterator().next());
					if ( r != null ) {
						result = r.getAmount();
					}
				}

			}
		}
		return result;
	}

	/*- =============================
	 *  DatumStreamsAccessor
	 *  ============================= */

	/**
	 * Get an earlier offset from the latest available datum per source ID.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, never {@code null}
	 */
	public Collection<DatumExpressionRoot> offsetMatching(String sourceIdPattern, int offset) {
		if ( datumStreamsAccessor == null || sourceIdPattern == null ) {
			return emptyList();
		}
		Collection<? extends Datum> found = datumStreamsAccessor.offsetMatching(sourceIdPattern, offset);
		if ( found == null || found.isEmpty() ) {
			return emptyList();
		}
		return found.stream().map(d -> copyWith(d)).toList();
	}

	/**
	 * Test if earlier datum exist offset from the latest available per source
	 * ID.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return {@code true} if at least one matching datum is available
	 */
	public boolean hasOffsetMatching(String sourceIdPattern, int offset) {
		if ( datumStreamsAccessor == null || sourceIdPattern == null ) {
			return false;
		}
		Collection<? extends Datum> found = datumStreamsAccessor.offsetMatching(sourceIdPattern, offset);
		return (found != null && !found.isEmpty());
	}

	/**
	 * Get a set of datum offset from a given timestamp, optionally matching a
	 * source ID pattern.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, never {@code null}
	 * @since 1.1
	 */
	public Collection<DatumExpressionRoot> offsetMatching(String sourceIdPattern, int offset,
			Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceIdPattern == null || timestamp == null ) {
			return emptyList();
		}
		Collection<? extends Datum> found = datumStreamsAccessor.offsetMatching(sourceIdPattern,
				timestamp, offset);
		if ( found == null || found.isEmpty() ) {
			return emptyList();
		}
		return found.stream().map(d -> copyWith(d)).toList();
	}

	/**
	 * Test if datum exist offset from a given timestamp, optionally matching a
	 * source ID pattern.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return {@code true} if at least one matching datum is available
	 * @since 1.1
	 */
	public boolean hasOffsetMatching(String sourceIdPattern, int offset, Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceIdPattern == null || timestamp == null ) {
			return false;
		}
		Collection<? extends Datum> found = datumStreamsAccessor.offsetMatching(sourceIdPattern,
				timestamp, offset);
		return (found != null && !found.isEmpty());
	}

	/**
	 * Get the latest available datum per source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offsetMatching(sourceIdPattern, 0)}.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @return the matching datum, never {@literal null}
	 * @see #offsetMatching(String, int)
	 */
	public Collection<DatumExpressionRoot> latestMatching(String sourceIdPattern) {
		return offsetMatching(sourceIdPattern, 0);
	}

	/**
	 * Test if datum exist.
	 *
	 * <p>
	 * This is equivalent to calling
	 * {@code hasOffsetMatching(sourceIdPattern, 0)}.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @return {@code true} if at least one matching datum is available
	 * @see #hasOffsetMatching(String, int)
	 */
	public boolean hasLatestMatching(String sourceIdPattern) {
		return hasOffsetMatching(sourceIdPattern, 0);
	}

	/**
	 * Get the latest available datum per source ID.
	 *
	 * <p>
	 * This is equivalent to calling
	 * {@code offsetMatching(sourceIdPattern, timestamp, 0)}.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, never {@literal null}
	 * @see #offsetMatching(String, int, Instant)
	 * @since 1.1
	 */
	public Collection<DatumExpressionRoot> latestMatching(String sourceIdPattern, Instant timestamp) {
		return offsetMatching(sourceIdPattern, 0, timestamp);
	}

	/**
	 * Test if datum exist.
	 *
	 * <p>
	 * This is equivalent to calling
	 * {@code hasOffsetMatching(sourceIdPattern, timestamp, 0)}.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return {@code true} if at least one matching datum is available
	 * @see #hasOffsetMatching(String, int , Instant)
	 * @since 1.1
	 */
	public boolean hasLatestMatching(String sourceIdPattern, Instant timestamp) {
		return hasOffsetMatching(sourceIdPattern, 0, timestamp);
	}

	/**
	 * Get an offset from the latest available datum matching a specific source
	 * ID.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot offset(String sourceId, int offset) {
		if ( datumStreamsAccessor == null || sourceId == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.offset(sourceId, offset);
		return (d != null ? copyWith(d) : null);
	}

	/**
	 * Test if an offset from the latest available datum matching a specific
	 * source ID exists.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return {@code true} if a matching datum exists
	 */
	public boolean hasOffset(String sourceId, int offset) {
		if ( datumStreamsAccessor == null || sourceId == null ) {
			return false;
		}
		Datum d = datumStreamsAccessor.offset(sourceId, offset);
		return (d != null);
	}

	/**
	 * Get an offset from the latest available datum matching a specific source
	 * ID.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 * @since 1.1
	 */
	public DatumExpressionRoot offset(String sourceId, int offset, Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceId == null || timestamp == null ) {
			return null;
		}
		Datum d = datumStreamsAccessor.offset(sourceId, timestamp, offset);
		return (d != null ? copyWith(d) : null);
	}

	/**
	 * Test if an offset from the latest available datum matching a specific
	 * source ID exists.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return {@code true} if a matching datum exists
	 * @since 1.1
	 */
	public boolean hasOffset(String sourceId, int offset, Instant timestamp) {
		if ( datumStreamsAccessor == null || sourceId == null || timestamp == null ) {
			return false;
		}
		Datum d = datumStreamsAccessor.offset(sourceId, timestamp, offset);
		return (d != null);
	}

	/**
	 * Get an offset from the latest available datum matching the source ID of
	 * this expression root.
	 *
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, or {@literal null} if not available
	 */
	public DatumExpressionRoot offset(int offset) {
		Datum me = getDatum();
		if ( me == null ) {
			return null;
		}
		return offset(me.getSourceId(), offset);
	}

	/**
	 * Test if an offset from the latest available datum matching the source ID
	 * of this expression root.
	 *
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return {@code true} if a matching datum exists
	 */
	public boolean hasOffset(int offset) {
		Datum me = getDatum();
		if ( me == null ) {
			return false;
		}
		return hasOffset(me.getSourceId(), offset);

	}

	/**
	 * Get datum offset from a given timestamp matching the source ID of this
	 * expression root.
	 *
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 * @since 1.1
	 */
	public DatumExpressionRoot offset(int offset, Instant timestamp) {
		Datum me = getDatum();
		if ( me == null ) {
			return null;
		}
		return offset(me.getSourceId(), offset, timestamp);
	}

	/**
	 * Test if datum offset from a given datum matching the source ID of this
	 * expression root exist.
	 *
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return {@code true} if a matching datum exists
	 * @since 1.1
	 */
	public boolean hasOffset(int offset, Instant timestamp) {
		Datum me = getDatum();
		if ( me == null ) {
			return false;
		}
		return hasOffset(me.getSourceId(), offset, timestamp);

	}

	/**
	 * Get the latest available datum matching a specific source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, 0)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to find
	 * @return the matching datum, or {@literal null} if not available
	 * @see #offset(String, int)
	 */
	public DatumExpressionRoot latest(String sourceId) {
		return offset(sourceId, 0);
	}

	/**
	 * Test if a datum matching a specific source ID exists.
	 *
	 * <p>
	 * This is equivalent to calling {@code hasOffset(sourceId, 0)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @return the matching datum, or {@literal null} if not available
	 * @return {@code true} if a matching datum exists
	 * @see #hasOffset(String, int)
	 */
	public boolean hasLatest(String sourceId) {
		return hasOffset(sourceId, 0);
	}

	/**
	 * Get datum latest to a reference timestamp matching a specific source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, 0, timestamp)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to find
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 * @see #offset(String, int, Instant)
	 * @since 1.1
	 */
	public DatumExpressionRoot latest(String sourceId, Instant timestamp) {
		return offset(sourceId, 0, timestamp);
	}

	/**
	 * Test if a datum latest to a reference timestamp matching a specific
	 * source ID exists.
	 *
	 * <p>
	 * This is equivalent to calling {@code hasOffset(sourceId, 0, timestamp)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@literal null} if not available
	 * @return {@code true} if a matching datum exists
	 * @see #hasOffset(String, int, Instant)
	 * @since 1.1
	 */
	public boolean hasLatest(String sourceId, Instant timestamp) {
		return hasOffset(sourceId, 0, timestamp);
	}

}
