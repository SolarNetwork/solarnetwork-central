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
import java.util.TimeZone;
import net.solarnetwork.central.dao.AggregationFilterableDao;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.PriceLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.dao.HardwareControlDatumDao;
import net.solarnetwork.central.datum.dao.PowerDatumDao;
import net.solarnetwork.central.datum.dao.PriceDatumDao;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateNodeDatumFilter;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.domain.WeatherConditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MutableInterval;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link QueryBiz}.
 * 
 * @author matt
 * @version 1.5
 */
public class DaoQueryBiz implements QueryBiz {

	private ConsumptionDatumDao consumptionDatumDao;
	private PowerDatumDao powerDatumDao;
	private SolarNodeDao solarNodeDao;
	private WeatherDatumDao weatherDatumDao;
	private DayDatumDao dayDatumDao;
	private GeneralNodeDatumDao generalNodeDatumDao;
	private int filteredResultsLimit = 1000;
	private long maxDaysForMinuteAggregation = 7;
	private long maxDaysForHourAggregation = 31;
	private long maxDaysForDayAggregation = 730;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>> daoMapping;
	private final Map<Class<? extends Datum>, FilterableDao<? extends EntityMatch, Long, ? extends DatumFilter>> filterDaoMapping;
	private final Map<Class<? extends Datum>, AggregationFilterableDao<?, ? extends AggregationFilter>> aggregationFilterDaoMapping;
	private final Map<Class<? extends Entity<?>>, FilterableDao<SourceLocationMatch, Long, SourceLocation>> filterLocationDaoMapping;

	/**
	 * Default constructor.
	 */
	public DaoQueryBiz() {
		super();
		daoMapping = new HashMap<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>>(4);
		filterDaoMapping = new HashMap<Class<? extends Datum>, FilterableDao<? extends EntityMatch, Long, ? extends DatumFilter>>(
				4);
		aggregationFilterDaoMapping = new HashMap<Class<? extends Datum>, AggregationFilterableDao<?, ? extends AggregationFilter>>(
				4);
		filterLocationDaoMapping = new HashMap<Class<? extends Entity<?>>, FilterableDao<SourceLocationMatch, Long, SourceLocation>>(
				4);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getReportableInterval(Long nodeId, Class<? extends NodeDatum>[] types) {
		MutableInterval interval = new MutableInterval(0, 0);
		for ( Class<? extends NodeDatum> clazz : types ) {
			ReadableInterval oneInterval = null;
			if ( consumptionDatumDao.getDatumType().isAssignableFrom(clazz) ) {
				oneInterval = consumptionDatumDao.getReportableInterval(nodeId);
			} else if ( powerDatumDao.getDatumType().isAssignableFrom(clazz) ) {
				oneInterval = powerDatumDao.getReportableInterval(nodeId);
			}
			if ( oneInterval != null ) {
				if ( interval.getEndMillis() == 0
						|| oneInterval.getEndMillis() > interval.getEndMillis() ) {
					interval.setEndMillis(oneInterval.getEndMillis());
				}
				if ( interval.getStartMillis() == 0
						|| oneInterval.getStartMillis() < interval.getStartMillis() ) {
					interval.setStartMillis(oneInterval.getStartMillis());
				}
			}
		}
		if ( interval.getStartMillis() == 0 ) {
			return null;
		}
		TimeZone tz = null;
		if ( nodeId != null ) {
			SolarNode node = solarNodeDao.get(nodeId);
			if ( node != null ) {
				tz = node.getTimeZone();
			}
		}
		return new ReportableInterval(interval, tz);
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

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, Class<? extends NodeDatum> type,
			LocalDate start, LocalDate end) {
		final Set<String> result;
		if ( consumptionDatumDao.getDatumType().isAssignableFrom(type) ) {
			result = consumptionDatumDao.getAvailableSources(nodeId, start, end);
		} else if ( powerDatumDao.getDatumType().isAssignableFrom(type) ) {
			result = powerDatumDao.getAvailableSources(nodeId, start, end);
		} else {
			result = Collections.emptySet();
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end) {
		return generalNodeDatumDao.getAvailableSources(nodeId, start, end);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReportableInterval getNetworkReportableInterval(Class<? extends NodeDatum>[] types) {
		return getReportableInterval(null, types);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<? extends NodeDatum> getAggregatedDatum(DatumQueryCommand criteria,
			Class<? extends NodeDatum> datumClass) {
		DatumDao<? extends NodeDatum> dao = daoMapping.get(datumClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Datum type "
					+ (datumClass == null ? "(null)" : datumClass.getSimpleName()) + " not supported");
		}
		if ( criteria.isMostRecent() ) {
			return dao.getMostRecentDatum(criteria);
		}
		Aggregation forced = enforceAggregation(criteria.getAggregation(), criteria.getStartDate(),
				criteria.getEndDate(), criteria);
		criteria.setAggregate(forced);
		return dao.getAggregatedDatum(criteria);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public WeatherConditions getMostRecentWeatherConditions(Long nodeId) {
		// get the SolarNode for the specified node, for the appropriate time zone
		SolarNode node = solarNodeDao.get(nodeId);

		WeatherDatum weather = weatherDatumDao.getMostRecentWeatherDatum(nodeId, new DateTime());
		DayDatum day = null;
		LocalTime infoTime = null;
		if ( weather instanceof ReportingDatum ) {
			ReportingDatum repWeather = (ReportingDatum) weather;
			day = dayDatumDao.getDatumForDate(nodeId, repWeather.getLocalDate());
			infoTime = repWeather.getLocalTime();
		} else if ( weather != null && weather.getInfoDate() != null ) {
			day = dayDatumDao.getDatumForDate(weather.getLocationId(), weather.getInfoDate());
			infoTime = weather.getInfoDate().toDateTime(DateTimeZone.forTimeZone(node.getTimeZone()))
					.toLocalTime();
		}
		if ( weather != null && day != null && infoTime != null
				&& (weather.getCondition() != null || day.getCondition() != null) ) {
			// check for night-time, this assumes all conditions set to day values from DAO
			if ( infoTime.isBefore(day.getSunrise()) || infoTime.isAfter(day.getSunset()) ) {
				// change to night-time
				if ( weather.getCondition() != null ) {
					weather.setCondition(weather.getCondition().getNightEquivalent());
				}
				if ( day.getCondition() != null ) {
					day.setCondition(weather.getCondition().getNightEquivalent());
				}
			}
		}
		return new WeatherConditions(weather, day, node.getTimeZone());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public <F extends DatumFilter> FilterResults<? extends EntityMatch> findFilteredDatum(F filter,
			Class<? extends Datum> datumClass, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		@SuppressWarnings("unchecked")
		FilterableDao<? extends EntityMatch, Long, F> dao = (FilterableDao<? extends EntityMatch, Long, F>) filterDaoMapping
				.get(datumClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Datum type "
					+ (datumClass == null ? "(null)" : datumClass.getSimpleName()) + " not supported");
		}
		return dao.findFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
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
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return generalNodeDatumDao.findAggregationFiltered(enforceGeneralAggregateLevel(filter),
				sortDescriptors, limitFilterOffset(offset), limitFilterMaximum(max));
	}

	private Aggregation enforceAggregation(Aggregation agg, final ReadableInstant s, ReadableInstant e,
			Filter filter) {
		Aggregation forced = null;
		long diffDays = (s != null && e != null ? (e.getMillis() - s.getMillis())
				/ (1000L * 60L * 60L * 24L) : 0);
		if ( s == null || e == null ) {
			if ( agg == null || agg.compareTo(Aggregation.Day) < 0 ) {
				log.info(
						"Restricting aggregate to Day level for filter with missing start or end date: {}",
						filter);
				forced = Aggregation.Day;
			}
		} else if ( diffDays > maxDaysForDayAggregation
				&& (agg == null || agg.compareLevel(Aggregation.Month) < 0) ) {
			log.info("Restricting aggregate to Month level for filter duration {} days (> {}): {}",
					diffDays, maxDaysForDayAggregation, filter);
			forced = Aggregation.Month;
		} else if ( diffDays > maxDaysForHourAggregation
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
		Aggregation forced = enforceAggregation(filter.getAggregation(), filter.getStartDate(),
				filter.getEndDate(), filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand();
			cmd.setAggregate(forced);
			cmd.setEndDate(filter.getEndDate());
			cmd.setNodeIds(filter.getNodeIds());
			cmd.setSourceIds(filter.getSourceIds());
			cmd.setStartDate(filter.getStartDate());
			return cmd;
		}
		return filter;
	}

	@SuppressWarnings("unchecked")
	private <A extends AggregationFilter> A enforceAggregateLevel(A filter) {
		Aggregation forced = enforceAggregation(filter.getAggregation(), filter.getStartDate(),
				filter.getEndDate(), filter);
		if ( forced != null ) {
			DatumFilterCommand cmd = new DatumFilterCommand();
			cmd.setAggregate(forced);
			cmd.setEndDate(filter.getEndDate());
			cmd.setStartDate(filter.getStartDate());
			if ( filter instanceof AggregateNodeDatumFilter ) {
				AggregateNodeDatumFilter andf = (AggregateNodeDatumFilter) filter;
				cmd.setNodeIds(andf.getNodeIds());
				cmd.setSourceIds(andf.getSourceIds());
			}
			// this cast is not pretty... but this is legacy code so we're going with it
			return (A) cmd;
		}
		return filter;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public <A extends AggregationFilter> FilterResults<?> findFilteredAggregateDatum(A filter,
			Class<? extends Datum> datumClass, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		@SuppressWarnings("unchecked")
		AggregationFilterableDao<?, A> dao = (AggregationFilterableDao<?, A>) aggregationFilterDaoMapping
				.get(datumClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Datum type "
					+ (datumClass == null ? "(null)" : datumClass.getSimpleName()) + " not supported");
		}
		filter = enforceAggregateLevel(filter);
		return dao.findAggregationFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<SourceLocationMatch> findFilteredLocations(SourceLocation filter,
			Class<? extends Entity<?>> locationClass, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		FilterableDao<SourceLocationMatch, Long, SourceLocation> dao = filterLocationDaoMapping
				.get(locationClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Entity type "
					+ (locationClass == null ? "(null)" : locationClass.getSimpleName())
					+ " not supported");
		}
		return dao.findFiltered(filter, sortDescriptors, limitFilterOffset(offset),
				limitFilterMaximum(max));
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

	@Autowired
	public void setDayDatumDao(DayDatumDao dayDatumDao) {
		this.dayDatumDao = dayDatumDao;
		daoMapping.put(dayDatumDao.getDatumType().asSubclass(NodeDatum.class), dayDatumDao);
		filterDaoMapping.put(dayDatumDao.getDatumType().asSubclass(Datum.class), dayDatumDao);
	}

	@Autowired
	public void setPowerDatumDao(PowerDatumDao powerDatumDao) {
		this.powerDatumDao = powerDatumDao;
		daoMapping.put(powerDatumDao.getDatumType(), powerDatumDao);
		filterDaoMapping.put(powerDatumDao.getDatumType(), powerDatumDao);
		aggregationFilterDaoMapping.put(powerDatumDao.getDatumType(), powerDatumDao);
	}

	@Autowired
	public void setWeatherDatumDao(WeatherDatumDao weatherDatumDao) {
		this.weatherDatumDao = weatherDatumDao;
		daoMapping.put(weatherDatumDao.getDatumType(), weatherDatumDao);
		filterDaoMapping.put(weatherDatumDao.getDatumType(), weatherDatumDao);
	}

	@Autowired
	public void setConsumptionDatumDao(ConsumptionDatumDao consumptionDatumDao) {
		this.consumptionDatumDao = consumptionDatumDao;
		daoMapping.put(consumptionDatumDao.getDatumType(), consumptionDatumDao);
		filterDaoMapping.put(consumptionDatumDao.getDatumType(), consumptionDatumDao);
		aggregationFilterDaoMapping.put(consumptionDatumDao.getDatumType(), consumptionDatumDao);
	}

	@Autowired
	public void setPriceDatumDao(PriceDatumDao priceDatumDao) {
		daoMapping.put(priceDatumDao.getDatumType(), priceDatumDao);
	}

	@Autowired
	public void setHardwareControlDatumDao(HardwareControlDatumDao hardwareControlDatumDao) {
		daoMapping.put(hardwareControlDatumDao.getDatumType(), hardwareControlDatumDao);
		filterDaoMapping.put(hardwareControlDatumDao.getDatumType(), hardwareControlDatumDao);
	}

	@Autowired
	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
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

	public void setMaxDaysForMinuteAggregation(long maxDaysForMinuteAggregation) {
		this.maxDaysForMinuteAggregation = maxDaysForMinuteAggregation;
	}

	public void setMaxDaysForHourAggregation(long maxDaysForHourAggregation) {
		this.maxDaysForHourAggregation = maxDaysForHourAggregation;
	}

	public void setMaxDaysForDayAggregation(long maxDaysForDayAggregation) {
		this.maxDaysForDayAggregation = maxDaysForDayAggregation;
	}

}
