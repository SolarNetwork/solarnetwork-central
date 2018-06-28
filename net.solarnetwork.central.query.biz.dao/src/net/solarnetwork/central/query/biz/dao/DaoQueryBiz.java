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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.PriceLocationDao;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.user.dao.UserNodeDao;

/**
 * Implementation of {@link QueryBiz}.
 * 
 * @author matt
 * @version 2.5
 */
public class DaoQueryBiz implements QueryBiz {

	private GeneralNodeDatumDao generalNodeDatumDao;
	private GeneralLocationDatumDao generalLocationDatumDao;
	private SolarLocationDao solarLocationDao;
	private UserNodeDao userNodeDao;
	private int filteredResultsLimit = 1000;
	private long maxDaysForMinuteAggregation = 7;
	private long maxDaysForHourAggregation = 31;
	private long maxDaysForDayAggregation = 730;
	private long maxDaysForDayOfWeekAggregation = 3650;
	private long maxDaysForHourOfDayAggregation = 3650;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<Class<? extends Entity<?>>, FilterableDao<SourceLocationMatch, Long, SourceLocation>> filterLocationDaoMapping;

	/**
	 * Default constructor.
	 */
	public DaoQueryBiz() {
		super();
		filterLocationDaoMapping = new HashMap<Class<? extends Entity<?>>, FilterableDao<SourceLocationMatch, Long, SourceLocation>>(
				4);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getReportableInterval(Long nodeId, String sourceId) {
		ReadableInterval interval = generalNodeDatumDao.getReportableInterval(nodeId, sourceId);
		if ( interval == null ) {
			return null;
		}
		DateTimeZone tz = null;
		if ( interval.getChronology() != null ) {
			tz = interval.getChronology().getZone();
		}
		return new ReportableInterval(interval, (tz == null ? null : tz.toTimeZone()));
	}

	@SuppressWarnings("deprecation")
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end) {
		return generalNodeDatumDao.getAvailableSources(nodeId, start, end);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(GeneralNodeDatumFilter filter) {
		return generalNodeDatumDao.getAvailableSources(filter);
	}

	@Override
	public Set<Long> findAvailableNodes(SecurityActor actor) {
		Set<Long> nodeIds = null;
		if ( actor instanceof SecurityNode ) {
			nodeIds = Collections.singleton(((SecurityNode) actor).getNodeId());
		} else if ( actor instanceof SecurityToken ) {
			String tokenId = ((SecurityToken) actor).getToken();
			nodeIds = userNodeDao.findNodeIdsForToken(tokenId);
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
		return generalNodeDatumDao.findFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return generalNodeDatumDao.findAggregationFiltered(enforceGeneralAggregateLevel(filter),
				sortDescriptors, limitFilterOffset(offset), limitFilterMaximum(max));
	}

	private Aggregation enforceAggregation(final Aggregation agg, ReadableInstant s, ReadableInstant e,
			Filter filter) {
		Aggregation forced = null;
		if ( agg == Aggregation.RunningTotal ) {
			// running total
			return null;
		}
		if ( s == null && e != null ) {
			// treat start date as SolarNetwork epoch (may want to make epoch configurable)
			s = new DateTime(2008, 1, 1, 0, 0, 0, DateTimeZone.UTC);
		} else if ( s != null && e == null ) {
			// treat end date as now for purposes of this calculating query range
			e = new DateTime();
		}
		long diffDays = (s != null && e != null
				? (e.getMillis() - s.getMillis()) / (1000L * 60L * 60L * 24L)
				: 0);
		if ( s == null && e == null && (agg == null || agg.compareTo(Aggregation.Day) < 0)
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
		return (forced != null ? forced : agg);
	}

	private AggregateGeneralNodeDatumFilter enforceGeneralAggregateLevel(
			AggregateGeneralNodeDatumFilter filter) {
		if ( filter.isMostRecent() ) {
			return filter;
		}
		Aggregation forced = enforceAggregation(filter.getAggregation(), filter.getStartDate(),
				filter.getEndDate(), filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand(filter);
			cmd.setAggregate(forced);
			return cmd;
		}
		return filter;
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
		return generalLocationDatumDao.findFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralLocationDatumMatch> findAggregateGeneralLocationDatum(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return generalLocationDatumDao.findAggregationFiltered(enforceGeneralAggregateLevel(filter),
				sortDescriptors, limitFilterOffset(offset), limitFilterMaximum(max));
	}

	private AggregateGeneralLocationDatumFilter enforceGeneralAggregateLevel(
			AggregateGeneralLocationDatumFilter filter) {
		Aggregation forced = enforceAggregation(filter.getAggregation(), filter.getStartDate(),
				filter.getEndDate(), filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand();
			cmd.setAggregate(forced);
			cmd.setEndDate(filter.getEndDate());
			cmd.setLocationIds(filter.getLocationIds());
			cmd.setSourceIds(filter.getSourceIds());
			cmd.setStartDate(filter.getStartDate());
			cmd.setDataPath(filter.getDataPath());
			return cmd;
		}
		return filter;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getLocationAvailableSources(Long locationId, DateTime start, DateTime end) {
		return generalLocationDatumDao.getAvailableSources(locationId, start, end);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getLocationReportableInterval(Long locationId, String sourceId) {
		ReadableInterval interval = generalLocationDatumDao.getReportableInterval(locationId, sourceId);
		if ( interval == null ) {
			return null;
		}
		DateTimeZone tz = null;
		if ( interval.getChronology() != null ) {
			tz = interval.getChronology().getZone();
		}
		return new ReportableInterval(interval, (tz == null ? null : tz.toTimeZone()));
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

	@Autowired
	public void setPriceLocationDao(PriceLocationDao priceLocationDao) {
		filterLocationDaoMapping.put(PriceLocation.class, priceLocationDao);
	}

	@Autowired
	public void setWeatherLocationDao(WeatherLocationDao weatherLocationDao) {
		filterLocationDaoMapping.put(WeatherLocation.class, weatherLocationDao);
	}

	public GeneralNodeDatumDao getGeneralNodeDatumDao() {
		return generalNodeDatumDao;
	}

	@Autowired
	public void setGeneralNodeDatumDao(GeneralNodeDatumDao generalNodeDatumDao) {
		this.generalNodeDatumDao = generalNodeDatumDao;
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

	public GeneralLocationDatumDao getGeneralLocationDatumDao() {
		return generalLocationDatumDao;
	}

	@Autowired
	public void setGeneralLocationDatumDao(GeneralLocationDatumDao generalLocationDatumDao) {
		this.generalLocationDatumDao = generalLocationDatumDao;
	}

	/**
	 * Set the user node DAO to use.
	 * 
	 * @param userNodeDao
	 *        the DAO
	 * @since 2.4
	 */
	@Autowired
	public void setUserNodeDao(UserNodeDao userNodeDao) {
		this.userNodeDao = userNodeDao;
	}

}
