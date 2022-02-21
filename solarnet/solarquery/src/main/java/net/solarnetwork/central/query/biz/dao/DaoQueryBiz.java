/* ===================================================================
 * DaoQueryBiz.java
 * 
 * Created Aug 5, 2009 12:31:45 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.central.query.biz.dao;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.toGeneralLocationDatum;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.toGeneralNodeDatum;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Implementation of {@link QueryBiz}.
 * 
 * @author matt
 * @version 4.0
 */
public class DaoQueryBiz implements QueryBiz {

	private final DatumEntityDao datumDao;
	private final DatumStreamMetadataDao metaDao;
	private final ReadingDatumDao readingDao;
	private SolarLocationDao solarLocationDao;
	private SolarNodeOwnershipDao nodeOwnershipDao;
	private Validator readingCriteriaValidator;
	private int filteredResultsLimit = 1000;
	private long maxDaysForMinuteAggregation = 7;
	private long maxDaysForHourAggregation = 31;
	private long maxDaysForDayAggregation = 730;
	private long maxDaysForDayOfWeekAggregation = 3650;
	private long maxDaysForHourOfDayAggregation = 3650;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param datumDao
	 *        the datum DAO
	 * @param metaDao
	 *        the metadata DAO
	 * @param readingDao
	 *        the reading DAO
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoQueryBiz(DatumEntityDao datumDao, DatumStreamMetadataDao metaDao,
			ReadingDatumDao readingDao, SolarNodeOwnershipDao nodeOwnershipDao) {
		super();
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
		this.readingDao = requireNonNullArgument(readingDao, "readingDao");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getReportableInterval(Long nodeId, String sourceId) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(nodeId);
		filter.setSourceId(sourceId);
		filter.setObjectKind(ObjectDatumKind.Node);
		Iterable<DatumDateInterval> results = datumDao.findAvailableInterval(filter);
		for ( DatumDateInterval interval : results ) {
			return new ReportableInterval(interval.getStart(), interval.getEnd(), interval.getZone());
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(GeneralNodeDatumFilter filter) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter);
		c.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(c);
		return stream(results.spliterator(), false).map(ObjectDatumStreamMetadata::getSourceId)
				.collect(toCollection(LinkedHashSet::new));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<NodeSourcePK> findAvailableSources(GeneralNodeDatumFilter filter) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter);
		c.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(c);
		return stream(results.spliterator(), false)
				.map(e -> new NodeSourcePK(e.getObjectId(), e.getSourceId()))
				.collect(toCollection(LinkedHashSet::new));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<NodeSourcePK> findAvailableSources(SecurityActor actor, DatumFilter filter) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter);
		if ( c == null ) {
			c = new BasicDatumCriteria();
		}
		c.setObjectKind(ObjectDatumKind.Node);
		if ( actor instanceof SecurityNode ) {
			Long nodeId = ((SecurityNode) actor).getNodeId();
			c.setNodeId(nodeId);
		} else if ( actor instanceof SecurityToken ) {
			String tokenId = ((SecurityToken) actor).getToken();
			c.setTokenId(tokenId);
		} else {
			return Collections.emptySet();
		}
		Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(c);
		return stream(results.spliterator(), false)
				.map(e -> new NodeSourcePK(e.getObjectId(), e.getSourceId()))
				.collect(toCollection(LinkedHashSet::new));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<Long> findAvailableNodes(SecurityActor actor) {
		Set<Long> nodeIds = null;
		if ( actor instanceof SecurityNode ) {
			nodeIds = Collections.singleton(((SecurityNode) actor).getNodeId());
		} else if ( actor instanceof SecurityToken ) {
			String tokenId = ((SecurityToken) actor).getToken();
			Long[] ids = nodeOwnershipDao.nonArchivedNodeIdsForToken(tokenId);
			nodeIds = (ids != null ? new LinkedHashSet<>(Arrays.asList(ids)) : null);
		}
		if ( nodeIds == null || nodeIds.isEmpty() ) {
			return Collections.emptySet();
		}
		return nodeIds;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<GeneralNodeDatumFilterMatch> findFilteredGeneralNodeDatum(
			GeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter, sortDescriptors,
				limitFilterOffset(offset), limitFilterMaximum(max));
		c.setObjectKind(ObjectDatumKind.Node);
		ObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = datumDao.findFiltered(c);
		List<GeneralNodeDatumFilterMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralNodeDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(enforceGeneralAggregateLevel(filter),
				sortDescriptors, limitFilterOffset(offset), limitFilterMaximum(max));
		c.setObjectKind(ObjectDatumKind.Node);
		ObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = datumDao.findFiltered(c);
		List<ReportingGeneralNodeDatumMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralNodeDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	private Aggregation enforceAggregation(final AggregationFilter filter) {
		final Aggregation agg = filter.getAggregation();
		Aggregation forced = null;
		if ( agg == Aggregation.RunningTotal ) {
			// running total
			return null;
		}
		Object startDate = null;
		Object endDate = null;
		long diffDays = 0;
		if ( filter instanceof LocalDateRangeFilter
				&& ((LocalDateRangeFilter) filter).getLocalStartDate() != null ) {
			LocalDateTime s = ((LocalDateRangeFilter) filter).getLocalStartDate();
			LocalDateTime e = ((LocalDateRangeFilter) filter).getLocalEndDate();
			if ( e == null ) {
				e = LocalDateTime.now();
			}
			startDate = s;
			endDate = e;
			diffDays = ChronoUnit.DAYS.between(s, e);
		} else {
			Instant s = filter.getStartDate();
			Instant e = filter.getEndDate();
			if ( s == null && e != null ) {
				// treat start date as SolarNetwork epoch (may want to make epoch configurable)
				s = LocalDateTime.of(2008, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC);
			} else if ( s != null && e == null ) {
				// treat end date as now for purposes of this calculating query range
				e = Instant.now();
			}
			startDate = s;
			endDate = e;
			diffDays = (s != null && e != null ? ChronoUnit.DAYS.between(s, e) : 0);
		}
		if ( startDate == null && endDate == null && (agg == null || agg.compareTo(Aggregation.Day) < 0)
				&& agg != Aggregation.HourOfDay && agg != Aggregation.SeasonalHourOfDay
				&& agg != Aggregation.DayOfWeek && agg != Aggregation.SeasonalDayOfWeek ) {
			log.info("Restricting aggregate to Day level for filter with missing start or end date: {}",
					filter);
			forced = Aggregation.Day;
		} else if ( agg == Aggregation.HourOfDay || agg == Aggregation.SeasonalHourOfDay ) {
			if ( diffDays > maxDaysForHourOfDayAggregation ) {
				log.info("Restricting aggregate to Month level for filter duration {} days (> {}): {}",
						diffDays, maxDaysForHourOfDayAggregation, filter);
				forced = Aggregation.Month;
			}
		} else if ( agg == Aggregation.DayOfWeek || agg == Aggregation.SeasonalDayOfWeek ) {
			if ( diffDays > maxDaysForDayOfWeekAggregation ) {
				log.info("Restricting aggregate to Month level for filter duration {} days (> {}): {}",
						diffDays, maxDaysForDayOfWeekAggregation, filter);
				forced = Aggregation.Month;
			}
		} else if ( maxDaysForDayAggregation > 0 && diffDays > maxDaysForDayAggregation
				&& (agg == null || agg.compareLevel(Aggregation.Month) < 0) ) {
			log.info("Restricting aggregate to Month level for filter duration {} days (> {}): {}",
					diffDays, maxDaysForDayAggregation, filter);
			forced = Aggregation.Month;
		} else if ( maxDaysForHourAggregation > 0 && diffDays > maxDaysForHourAggregation
				&& (agg == null || agg.compareLevel(Aggregation.Day) < 0) ) {
			log.info("Restricting aggregate to Day level for filter duration {} days (> {}): {}",
					diffDays, maxDaysForHourAggregation, filter);
			forced = Aggregation.Day;
		} else if ( diffDays > maxDaysForMinuteAggregation
				&& (agg == null || agg.compareTo(Aggregation.Hour) < 0) ) {
			log.info("Restricting aggregate to Hour level for filter duration {} days (> {}): {}",
					diffDays, maxDaysForMinuteAggregation, filter);
			forced = Aggregation.Hour;
		}
		return forced;
	}

	private AggregateGeneralNodeDatumFilter enforceGeneralAggregateLevel(
			AggregateGeneralNodeDatumFilter filter) {
		if ( filter.isMostRecent() ) {
			return filter;
		}
		Aggregation forced = enforceAggregation(filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand(filter);
			cmd.setAggregate(forced);
			return cmd;
		}
		return filter;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateReading(
			AggregateGeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		if ( readingType != DatumReadingType.Difference ) {
			throw new IllegalArgumentException("The DatumReadingType [" + readingType
					+ "] is not supported for aggregate level [" + filter.getAggregation() + "]");
		}
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(enforceGeneralAggregateLevel(filter),
				sortDescriptors, offset, max);
		c.setObjectKind(ObjectDatumKind.Node);
		c.setReadingType(readingType);
		validateReadingCriteria(c);
		ObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = datumDao.findFiltered(c);
		List<ReportingGeneralNodeDatumMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralNodeDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredReading(
			GeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter);
		c.setObjectKind(ObjectDatumKind.Node);
		c.setReadingType(readingType);
		c.setTimeTolerance(tolerance);
		validateReadingCriteria(c);
		ObjectDatumStreamFilterResults<ReadingDatum, DatumPK> daoResults = readingDao
				.findDatumReadingFiltered(c);
		List<ReportingGeneralNodeDatumMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralNodeDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	private void validateReadingCriteria(ReadingDatumCriteria criteria) {
		Validator v = (readingCriteriaValidator != null
				&& readingCriteriaValidator.supports(criteria.getClass()) ? readingCriteriaValidator
						: null);
		if ( v == null ) {
			return;
		}
		Errors errors = new BindException(criteria, "filter");
		v.validate(criteria, errors);
		if ( errors.hasErrors() ) {
			throw new ValidationException(errors);
		}

		if ( !criteria.hasDateOrLocalDateRange() ) {
			throw new IllegalArgumentException("A date range is required.");
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<LocationMatch> findFilteredLocations(Location filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		if ( filter == null || filter.getFilter() == null || filter.getFilter().isEmpty() ) {
			throw new IllegalArgumentException("Filter is required.");
		}
		return solarLocationDao.findFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<GeneralLocationDatumFilterMatch> findGeneralLocationDatum(
			GeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter, sortDescriptors,
				limitFilterOffset(offset), limitFilterMaximum(max));
		c.setObjectKind(ObjectDatumKind.Location);
		ObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = datumDao.findFiltered(c);
		List<GeneralLocationDatumFilterMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralLocationDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralLocationDatumMatch> findAggregateGeneralLocationDatum(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(enforceGeneralAggregateLevel(filter),
				sortDescriptors, limitFilterOffset(offset), limitFilterMaximum(max));
		c.setObjectKind(ObjectDatumKind.Location);
		ObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = datumDao.findFiltered(c);
		List<ReportingGeneralLocationDatumMatch> data = stream(daoResults.spliterator(), false)
				.map(e -> toGeneralLocationDatum(e, daoResults.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		return new BasicFilterResults<>(data, daoResults.getTotalResults(),
				daoResults.getStartingOffset(), daoResults.getReturnedResultCount());
	}

	private AggregateGeneralLocationDatumFilter enforceGeneralAggregateLevel(
			AggregateGeneralLocationDatumFilter filter) {
		Aggregation forced = enforceAggregation(filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand(filter, new SolarLocation());
			cmd.setAggregate(forced);
			cmd.setLocationIds(filter.getLocationIds());
			return cmd;
		}
		return filter;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getLocationAvailableSources(Long locationId, Instant start, Instant end) {
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setLocationId(locationId);
		c.setStartDate(start);
		c.setEndDate(end);
		c.setObjectKind(ObjectDatumKind.Location);
		Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(c);
		return stream(results.spliterator(), false).map(ObjectDatumStreamMetadata::getSourceId)
				.collect(toCollection(LinkedHashSet::new));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getLocationReportableInterval(Long locationId, String sourceId) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(locationId);
		filter.setSourceId(sourceId);
		filter.setObjectKind(ObjectDatumKind.Location);
		Iterable<DatumDateInterval> results = datumDao.findAvailableInterval(filter);
		for ( DatumDateInterval interval : results ) {
			return new ReportableInterval(interval.getStart(), interval.getEnd(), interval.getZone());
		}
		return null;
	}

	private Integer limitFilterMaximum(Integer requestedMaximum) {
		if ( requestedMaximum == null || requestedMaximum.intValue() > filteredResultsLimit
				|| requestedMaximum.intValue() < 1 ) {
			return filteredResultsLimit;
		}
		return requestedMaximum;
	}

	private Integer limitFilterOffset(Integer requestedOffset) {
		if ( requestedOffset == null || requestedOffset.intValue() < 0 ) {
			return 0;
		}
		return requestedOffset;
	}

	public int getFilteredResultsLimit() {
		return filteredResultsLimit;
	}

	public void setFilteredResultsLimit(int filteredResultsLimit) {
		this.filteredResultsLimit = filteredResultsLimit;
	}

	/**
	 * Set the maximum hour time range allowed for minute aggregate queries
	 * before a higher aggregation level (e.g. hour) is enforced.
	 * 
	 * @param maxDaysForMinuteAggregation
	 *        the maximum hour range, or {@literal 0} to not restrict; defaults
	 *        to {@literal 7}
	 */
	public void setMaxDaysForMinuteAggregation(long maxDaysForMinuteAggregation) {
		this.maxDaysForMinuteAggregation = maxDaysForMinuteAggregation;
	}

	/**
	 * Set the maximum hour time range allowed for hour aggregate queries before
	 * a higher aggregation level (e.g. day) is enforced.
	 * 
	 * @param maxDaysForHourAggregation
	 *        the maximum hour range, or {@literal 0} to not restrict; defaults
	 *        to {@literal 31}
	 */
	public void setMaxDaysForHourAggregation(long maxDaysForHourAggregation) {
		this.maxDaysForHourAggregation = maxDaysForHourAggregation;
	}

	/**
	 * Set the maximum hour time range allowed for day aggregate queries before
	 * a higher aggregation level (e.g. month) is enforced.
	 * 
	 * @param maxDaysForDayAggregation
	 *        the maximum hour range, or {@literal 0} to not restrict; defaults
	 *        to {@literal 730}
	 */
	public void setMaxDaysForDayAggregation(long maxDaysForDayAggregation) {
		this.maxDaysForDayAggregation = maxDaysForDayAggregation;
	}

	public void setMaxDaysForDayOfWeekAggregation(long maxDaysForDayOfWeekAggregation) {
		this.maxDaysForDayOfWeekAggregation = maxDaysForDayOfWeekAggregation;
	}

	public void setMaxDaysForHourOfDayAggregation(long maxDaysForHourOfDayAggregation) {
		this.maxDaysForHourOfDayAggregation = maxDaysForHourOfDayAggregation;
	}

	public SolarLocationDao getSolarLocationDao() {
		return solarLocationDao;
	}

	@Autowired
	public void setSolarLocationDao(SolarLocationDao solarLocationDao) {
		this.solarLocationDao = solarLocationDao;
	}

}
