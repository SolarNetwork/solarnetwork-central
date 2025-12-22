/* ==================================================================
 * QueryingDatumStreamsAccessor.java - 19/02/2025 1:51:51 pm
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

package net.solarnetwork.central.datum.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PathMatcher;
import org.threeten.extra.Interval;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Extension of {@link BasicDatumStreamsAccessor} that supports querying for
 * "missing" datum using a {@link DatumEntityDao}.
 *
 * @author matt
 * @version 1.3
 */
public class QueryingDatumStreamsAccessor extends BasicDatumStreamsAccessor {

	private static final Logger log = LoggerFactory.getLogger(QueryingDatumStreamsAccessor.class);

	/** Sort by stream ascending then date descending. */
	public static final List<SortDescriptor> SORT_BY_DATE_DESCENDING = List.of(
			new SimpleSortDescriptor(DatumSqlUtils.SORT_BY_STREAM),
			new SimpleSortDescriptor(DatumSqlUtils.SORT_BY_TIME, true));

	/** The maximum start date duration to use when querying. */
	public static final Duration DEFAULT_MAX_START_DATE_DURATION = Duration.ofDays(90);

	/** The maximum number of datum allowed to be queried from the database. */
	public static final int DEFAULT_MAX_RESULTS = 100;

	private final Map<ObjectDatumKind, Map<Long, Map<String, Interval>>> loadedRanges = new HashMap<>(4);

	private final Long userId;
	private final InstantSource clock;
	private final DatumEntityDao datumDao;
	private final DatumStreamMetadataDao metaDao;
	private final QueryAuditor auditor;

	private Duration maxStartDateDuration = DEFAULT_MAX_START_DATE_DURATION;
	private int maxResults = DEFAULT_MAX_RESULTS;

	/**
	 * Constructor.
	 *
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param datum
	 *        the datum list
	 * @param userId
	 *        the ID of the user that owns all datum
	 * @param clock
	 *        the clock to use
	 * @param datumDao
	 *        the datum DAO
	 * @param metaDao
	 *        the datum stream metadata DAO
	 * @param auditor
	 *        the optional auditor
	 * @throws IllegalArgumentException
	 *         if {@code pathMatcher} or {@code datumDao} or {@literal null}
	 */
	public QueryingDatumStreamsAccessor(PathMatcher pathMatcher, Collection<? extends Datum> datum,
			Long userId, InstantSource clock, DatumEntityDao datumDao, DatumStreamMetadataDao metaDao,
			QueryAuditor auditor) {
		super(pathMatcher, datum);
		this.userId = requireNonNullArgument(userId, "userId");
		this.clock = requireNonNullArgument(clock, "clock");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
		this.auditor = auditor;
	}

	@Override
	protected Datum offsetMiss(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			int offset) {
		final Datum oldestDatum = (!list.isEmpty() ? list.getLast() : null);
		final int max = 1 + (offset > list.size() ? offset - list.size() : offset);

		return query(kind, objectId, sourceId, list, oldestDatum, null, max);
	}

	@Override
	protected Datum offsetMiss(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			Instant timestamp, int offset, int referenceIndex) {
		final Datum oldestDatum = (!list.isEmpty() ? list.getLast() : null);
		final int max = 1 + (referenceIndex >= 0 ? offset - list.size() + referenceIndex : offset);

		return query(kind, objectId, sourceId, list, oldestDatum, timestamp, max);
	}

	private Datum query(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			Datum oldestDatum, Instant timestamp, int max) {
		final int maxAllowedResults = getMaxResults();
		final Long userId = kind == ObjectDatumKind.Node ? this.userId : null;

		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setObjectKind(kind);
		if ( kind == ObjectDatumKind.Node ) {
			c.setNodeId(objectId);
		} else {
			c.setLocationId(objectId);
		}
		c.setSourceId(sourceId);
		c.setSorts(SORT_BY_DATE_DESCENDING);
		c.setUserId(userId);

		if ( oldestDatum != null
				&& (timestamp == null || !timestamp.isBefore(oldestDatum.getTimestamp())) ) {
			c.setEndDate(oldestDatum.getTimestamp()); // < existing
		} else if ( timestamp != null ) {
			c.setEndDate(timestamp.plusMillis(1)); // <= timestamp
		} else {
			c.setEndDate(clock.instant().plusMillis(1)); // <= now
		}
		c.setStartDate(c.getEndDate().minus(maxStartDateDuration));

		// set max to 1 < max < maxAllowed
		c.setMax(Math.max(1, maxAllowedResults > 0 ? Math.min(max, maxAllowedResults) : max));

		ObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK> daoResults = datumDao
				.findFiltered(c);

		log.debug("Query user {} node {} source [{}] for {} between {} - {} found {}", userId, objectId,
				sourceId, c.getMax(), c.getStartDate(), c.getEndDate(),
				daoResults.getReturnedResultCount());

		final QueryAuditor auditor = (kind == ObjectDatumKind.Node ? this.auditor : null);

		if ( oldestDatum != null && timestamp != null && timestamp.isBefore(oldestDatum.getTimestamp())
				&& daoResults.getReturnedResultCount() > 0 ) {
			// gap fill any between oldest existing and timestamp
			var firstFound = daoResults.iterator().next();
			BasicDatumCriteria gfc = c.clone();
			gfc.setEndDate(oldestDatum.getTimestamp());
			gfc.setStartDate(firstFound.getTimestamp().plusMillis(1));
			gfc.setMax(maxAllowedResults);
			var gapFillResults = datumDao.findFiltered(gfc);

			log.debug("Gap fill query user {} node {} source [{}] for {} between {} - {} found {}",
					userId, objectId, sourceId, gfc.getMax(), gfc.getStartDate(), gfc.getEndDate(),
					gapFillResults.getReturnedResultCount());

			processQueryResults(userId, kind, objectId, sourceId, list, gapFillResults, auditor);
		}

		processQueryResults(userId, kind, objectId, sourceId, list, daoResults, auditor);

		return (daoResults.getReturnedResultCount() == max ? list.getLast() : null);
	}

	private void processQueryResults(final Long userId, ObjectDatumKind kind, Long objectId,
			String sourceId, List<Datum> list,
			ObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK> daoResults,
			final QueryAuditor auditor) {
		for ( var daoDatum : daoResults ) {
			ObjectDatumStreamMetadata meta = daoResults.metadataForStreamId(daoDatum.getStreamId());
			var d = ObjectDatum.forStreamDatum(daoDatum, userId,
					new DatumId(kind, objectId, sourceId, daoDatum.getTimestamp()), meta);
			if ( auditor != null ) {
				auditor.auditNodeDatum(d);
			}
			list.add(d);
		}
	}

	@Override
	protected Datum atMiss(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			Instant timestamp, int referenceIndex) {
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setObjectKind(kind);
		if ( kind == ObjectDatumKind.Node ) {
			c.setNodeId(objectId);
		} else {
			c.setLocationId(objectId);
		}
		c.setSourceId(sourceId);
		c.setUserId(userId);
		c.setStartDate(timestamp);
		c.setEndDate(timestamp.plusMillis(1));
		c.setMax(1);

		ObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK> daoResults = datumDao
				.findFiltered(c);

		log.debug("Query user {} node {} source [{}] for {} at {} found {}", userId, objectId, sourceId,
				c.getMax(), c.getStartDate(), daoResults.getReturnedResultCount());

		if ( daoResults.getReturnedResultCount() < 1 ) {
			return null;
		}

		final QueryAuditor auditor = (kind == ObjectDatumKind.Node ? this.auditor : null);

		var daoDatum = daoResults.iterator().next();
		ObjectDatumStreamMetadata meta = daoResults.metadataForStreamId(daoDatum.getStreamId());
		var d = ObjectDatum.forStreamDatum(daoDatum, userId,
				new DatumId(kind, objectId, sourceId, daoDatum.getTimestamp()), meta);
		if ( auditor != null ) {
			auditor.auditNodeDatum(d);
		}
		if ( referenceIndex >= list.size() ) {
			list.add(d);
		} else {
			list.add(referenceIndex, d);
		}
		return d;
	}

	private boolean isRangeLoaded(ObjectDatumKind kind, Long objectId, String sourceId, Instant from,
			Instant to) {
		final var nodeSourceRangeMap = loadedRanges.get(kind);
		if ( nodeSourceRangeMap == null ) {
			return false;
		}

		final var sourceRangeMap = nodeSourceRangeMap.get(objectId);
		if ( sourceRangeMap == null ) {
			return false;
		}

		final var range = sourceRangeMap.get(sourceId);
		if ( range == null ) {
			return false;
		}

		return range.startsAtOrBefore(from) && range.endsAtOrAfter(to);
	}

	private void markRangeLoaded(ObjectDatumKind kind, Long objectId, String sourceId, Instant from,
			Instant to) {
		// @formatter:off
		loadedRanges.computeIfAbsent(kind, _ -> new HashMap<>(4))
				.computeIfAbsent(objectId, _ -> new HashMap<>(4))
				.put(sourceId, Interval.of(from, to));
		// @formatter:on
	}

	@Override
	protected Collection<Datum> rangeMatching(ObjectDatumKind kind, Long objectId,
			String sourceIdPattern, Instant from, Instant to, Map<String, List<Datum>> datumBySourceId) {
		// check if already loaded this range, to avoid loading again
		if ( isRangeLoaded(kind, objectId, sourceIdPattern, from, to) ) {
			return super.rangeMatching(kind, objectId, sourceIdPattern, from, to, datumBySourceId);
		}

		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setObjectKind(kind);
		if ( kind == ObjectDatumKind.Node ) {
			c.setNodeId(objectId);
		} else {
			c.setLocationId(objectId);
		}
		c.setSourceId(sourceIdPattern);
		c.setUserId(userId);
		c.setStartDate(from);
		c.setEndDate(to);
		c.setMax(maxResults);
		c.setSorts(SORT_BY_DATE_DESCENDING);

		ObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK> daoResults = datumDao
				.findFiltered(c);
		if ( daoResults == null ) {
			// not expected
			return List.of();
		}

		log.debug("Query user {} node {} source [{}] range [{} - {}] found {}", userId, objectId,
				sourceIdPattern, from, to, daoResults.getReturnedResultCount());

		if ( daoResults.getReturnedResultCount() < 1 ) {
			return null;
		}

		final QueryAuditor auditor = (kind == ObjectDatumKind.Node ? this.auditor : null);

		final List<Datum> result = new ArrayList<>(daoResults.getReturnedResultCount());
		final Set<String> seenSourceIds = new HashSet<>(4);

		for ( var daoDatum : daoResults ) {
			ObjectDatumStreamMetadata meta = daoResults.metadataForStreamId(daoDatum.getStreamId());
			var d = ObjectDatum.forStreamDatum(daoDatum, userId,
					new DatumId(kind, objectId, meta.getSourceId(), daoDatum.getTimestamp()), meta);
			if ( auditor != null ) {
				auditor.auditNodeDatum(d);
			}

			result.add(d);
			if ( !seenSourceIds.contains(d.getSourceId()) ) {
				seenSourceIds.add(d.getSourceId());
			}

			final List<Datum> list = datumBySourceId.computeIfAbsent(d.getSourceId(),
					_ -> new ArrayList<>(daoResults.getReturnedResultCount()));
			list.add(d);
		}

		// re-sort datum lists, remove any duplicates
		for ( String sourceId : seenSourceIds ) {
			List<Datum> list = datumBySourceId.get(sourceId);
			list.sort((l, r) -> r.getTimestamp().compareTo(l.getTimestamp()));
			Datum prev = null;
			for ( ListIterator<Datum> itr = list.listIterator(); itr.hasNext(); ) {
				Datum d = itr.next();
				if ( prev != null ) {
					if ( prev.getTimestamp().equals(d.getTimestamp()) ) {
						itr.remove();
					}
				}
				prev = d;
			}
		}

		markRangeLoaded(kind, objectId, sourceIdPattern, from, to);

		return result;

	}

	@Override
	public Collection<ObjectDatumStreamMetadata> findStreams(ObjectDatumKind kind, String query,
			String sourceIdPattern, String... tags) {
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setObjectKind(kind);
		criteria.setSourceId(sourceIdPattern);
		if ( query != null && kind == ObjectDatumKind.Location ) {
			SimpleLocation loc = new SimpleLocation();
			loc.setName(query);
			criteria.setLocation(loc);
		}
		criteria.setSearchFilter(DatumUtils.generateTagsSearchFilter(tags));
		Iterable<ObjectDatumStreamMetadata> data = metaDao.findDatumStreamMetadata(criteria);
		if ( data instanceof Collection<ObjectDatumStreamMetadata> col ) {
			return col;
		}
		return StreamSupport.stream(data.spliterator(), false).toList();
	}

	/**
	 * Get the maximum start date duration.
	 *
	 * @return the maxStartDateDuration the duration, never {@code null};
	 *         defaults to {@link #DEFAULT_MAX_START_DATE_DURATION}
	 */
	public final Duration getMaxStartDateDuration() {
		return maxStartDateDuration;
	}

	/**
	 * Set the maximum start date duration.
	 *
	 * @param maxStartDateDuration
	 *        the duration to set; if {@code null} or not positive then
	 *        {@link #DEFAULT_MAX_START_DATE_DURATION} will be set
	 */
	public final void setMaxStartDateDuration(Duration maxStartDateDuration) {
		this.maxStartDateDuration = maxStartDateDuration != null && maxStartDateDuration.isPositive()
				? maxStartDateDuration
				: DEFAULT_MAX_START_DATE_DURATION;
	}

	/**
	 * Get the maximum results to allow to be queried.
	 *
	 * @return the max, or {@code 0} for no limit; defaults to
	 *         {@link #DEFAULT_MAX_RESULTS}
	 */
	public final int getMaxResults() {
		return maxResults;
	}

	/**
	 * Set the maximum results to allow to be queried.
	 *
	 * @param maxResults
	 *        the maximum to set, or {@code 0} for no limit
	 */
	public final void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

}
