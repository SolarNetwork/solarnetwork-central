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
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.PriceLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.dao.HardwareControlDatumDao;
import net.solarnetwork.central.datum.dao.PowerDatumDao;
import net.solarnetwork.central.datum.dao.PriceDatumDao;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.EntityMatch;
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
import org.joda.time.ReadableInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link QueryBiz}.
 * 
 * @author matt
 * @version 1.1
 */
public class DaoQueryBiz implements QueryBiz {

	private ConsumptionDatumDao consumptionDatumDao;
	private PowerDatumDao powerDatumDao;
	private SolarNodeDao solarNodeDao;
	private WeatherDatumDao weatherDatumDao;
	private DayDatumDao dayDatumDao;
	private int filteredResultsLimit = 250;

	private final Map<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>> daoMapping;
	private final Map<Class<? extends Datum>, FilterableDao<? extends EntityMatch, Long, ? extends DatumFilter>> filterDaoMapping;
	private final Map<Class<? extends Entity<?>>, FilterableDao<SourceLocationMatch, Long, SourceLocation>> filterLocationDaoMapping;

	/**
	 * Default constructor.
	 */
	public DaoQueryBiz() {
		super();
		daoMapping = new HashMap<Class<? extends NodeDatum>, DatumDao<? extends NodeDatum>>(4);
		filterDaoMapping = new HashMap<Class<? extends Datum>, FilterableDao<? extends EntityMatch, Long, ? extends DatumFilter>>(
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
	public ReportableInterval getNetworkReportableInterval(Class<? extends NodeDatum>[] types) {
		return getReportableInterval(null, types);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<? extends NodeDatum> getAggregatedDatum(Class<? extends NodeDatum> datumClass,
			DatumQueryCommand criteria) {
		DatumDao<? extends NodeDatum> dao = daoMapping.get(datumClass);
		if ( dao == null ) {
			throw new IllegalArgumentException("Datum type "
					+ (datumClass == null ? "(null)" : datumClass.getSimpleName()) + " not supported");
		}
		if ( criteria.isMostRecent() ) {
			return dao.getMostRecentDatum(criteria);
		}
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
	public <F extends DatumFilter> FilterResults<? extends EntityMatch> findFilteredDatum(
			Class<? extends Datum> datumClass, F filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
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
	public FilterResults<SourceLocationMatch> findFilteredLocations(
			Class<? extends Entity<?>> locationClass, SourceLocation filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
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
		daoMapping.put(powerDatumDao.getDatumType().asSubclass(NodeDatum.class), powerDatumDao);
	}

	@Autowired
	public void setWeatherDatumDao(WeatherDatumDao weatherDatumDao) {
		this.weatherDatumDao = weatherDatumDao;
		daoMapping.put(weatherDatumDao.getDatumType().asSubclass(NodeDatum.class), weatherDatumDao);
		filterDaoMapping.put(weatherDatumDao.getDatumType().asSubclass(Datum.class), weatherDatumDao);
	}

	@Autowired
	public void setConsumptionDatumDao(ConsumptionDatumDao consumptionDatumDao) {
		this.consumptionDatumDao = consumptionDatumDao;
		daoMapping.put(consumptionDatumDao.getDatumType().asSubclass(NodeDatum.class),
				consumptionDatumDao);
		filterDaoMapping.put(consumptionDatumDao.getDatumType().asSubclass(Datum.class),
				consumptionDatumDao);
	}

	@Autowired
	public void setPriceDatumDao(PriceDatumDao priceDatumDao) {
		daoMapping.put(priceDatumDao.getDatumType().asSubclass(NodeDatum.class), priceDatumDao);
	}

	@Autowired
	public void setHardwareControlDatumDao(HardwareControlDatumDao hardwareControlDatumDao) {
		daoMapping.put(hardwareControlDatumDao.getDatumType().asSubclass(NodeDatum.class),
				hardwareControlDatumDao);
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

}
